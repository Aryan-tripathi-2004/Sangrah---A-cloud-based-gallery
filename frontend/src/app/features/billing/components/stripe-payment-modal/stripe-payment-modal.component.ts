import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, OnInit, OnDestroy, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BillingService } from '../../billing.service';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';

@Component({
  selector: 'app-stripe-payment-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stripe-payment-modal.component.html',
  styles: [`
    :host {
      --tw-blur: blur(8px);
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .animate-spin {
      animation: spin 1s linear infinite;
    }
  `]
})
export class StripePaymentModalComponent implements OnInit, OnDestroy {
  @Input() invoiceId!: string;
  @Output() closeModal = new EventEmitter<void>();
  @Output() paymentSuccess = new EventEmitter<void>();
  @ViewChild('stripeContainer') stripeContainerRef!: ElementRef;
  @ViewChild('addressContainer') addressContainerRef!: ElementRef;  // NEW: Address element container

  paymentIntent$: Observable<any>;

  stripe: Stripe | null = null;
  elements: StripeElements | null = null;
  paymentElement: StripePaymentElement | null = null;
  addressElement: any = null;  // Address element for billing details

  stripeReady = false;
  processing = false;
  paymentError = '';
  showSuccessMessage = false;
  showProcessingMessage = false;  // NEW: Show "waiting for confirmation" message
  timeoutCountdown = 300;  // NEW: 5 minutes = 300 seconds

  private destroy$ = new Subject<void>();
  private webhookSocket: WebSocket | null = null;  // NEW: WebSocket for real-time updates
  private webhookTimeout: any = null;  // NEW: Timeout handler
  private countdownInterval: any = null;  // NEW: Countdown timer

  constructor(
    private billingService: BillingService,
    private ngZone: NgZone
  ) {
    this.paymentIntent$ = this.billingService.paymentIntent$;
  }

  async ngOnInit(): Promise<void> {
    try {
      // Initialize Stripe
      const stripeKey = this.billingService.getStripePublicKey();
      console.log('🔑 Stripe Key Check:', stripeKey?.substring(0, 20) + '...');

      if (!stripeKey || stripeKey.includes('YOUR_TEST_KEY')) {
        this.paymentError = 'Stripe is not configured. Please set up your Stripe test key.';
        console.error('❌ Stripe key not configured:', stripeKey);
        return;
      }

      this.stripe = await loadStripe(stripeKey);
      if (!this.stripe) {
        this.paymentError = 'Failed to initialize Stripe. Please try again.';
        console.error('❌ Failed to load Stripe');
        return;
      }

      console.log('✅ Stripe loaded successfully');

      // Create payment intent on backend
      console.log('💳 Creating payment intent for invoice:', this.invoiceId);
      this.billingService
        .createPaymentIntent(this.invoiceId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            console.log('✅ Payment intent created:', response);
            // Payment intent created, now setup Elements
            this.setupPaymentElements();
          },
          error: (err) => {
            console.error('❌ Error creating payment intent:', err);
            console.error('   Status:', err.status);
            console.error('   Message:', err.message);
            console.error('   Body:', err.error);
            this.paymentError = `Failed to initialize payment: ${err.error?.message || err.message || 'Unknown error'}`;
          }
        });
    } catch (error) {
      console.error('❌ Error initializing Stripe:', error);
      this.paymentError = 'Payment initialization failed. Please try again.';
    }
  }

  /**
   * Set up Stripe Elements with client secret
   */
  private setupPaymentElements(): void {
    console.log('🔧 Setting up payment elements...');

    if (!this.stripe) {
      this.paymentError = 'Stripe not initialized';
      console.error('❌ Stripe instance is null');
      return;
    }

    try {
      const paymentIntent = this.billingService.getCurrentPaymentIntent();
      console.log('💰 Current payment intent:', paymentIntent);

      if (!paymentIntent || !paymentIntent.clientSecret) {
        this.paymentError = 'Payment session not ready. Please try again.';
        console.error('❌ No payment intent or client secret');
        return;
      }

      console.log('🎫 Client secret:', paymentIntent.clientSecret.substring(0, 20) + '...');

      // Create Elements instance with billing details collection enabled
      this.elements = this.stripe.elements({
        clientSecret: paymentIntent.clientSecret,
        appearance: {
          theme: 'night',
          variables: {
            colorPrimary: '#3b82f6',
            colorBackground: '#1e293b',
            colorText: '#f1f5f9',
            colorDanger: '#ef4444',
            fontFamily: 'system-ui, -apple-system, sans-serif',
            borderRadius: '6px',
            spacingUnit: '4px'
          }
        }
      });

      console.log('✅ Elements instance created');

      // Create ADDRESS element first (collects name, email, address)
      this.addressElement = this.elements.create('address', {
        mode: 'billing',  // Collect billing address
        defaultValues: {
          address: {
            country: 'IN'  // Default to India
          }
        }
      });
      console.log('✅ Address element created');

      // Mount address element FIRST
      if (this.addressContainerRef && this.addressContainerRef.nativeElement) {
        console.log('📌 Mounting address element...');
        this.addressElement.mount(this.addressContainerRef.nativeElement);
        console.log('✅ Address element mounted!');
      } else {
        console.error('❌ Address container ref is not available');
        this.paymentError = 'Address form setup failed. Please try again.';
        return;
      }

      // Create PAYMENT element
      // The payment element automatically collects billing details (name, email, address)
      // because the payment intent has a customer attached
      this.paymentElement = this.elements.create('payment');
      console.log('✅ Payment element created with auto billing details collection');

      // Mount payment element - container is always in DOM now
      if (this.stripeContainerRef && this.stripeContainerRef.nativeElement) {
        console.log('📌 Mounting payment element...');
        this.paymentElement.mount(this.stripeContainerRef.nativeElement);
        this.stripeReady = true;
        console.log('✅ Payment element mounted and ready!');
      } else {
        console.error('❌ Container ref is not available - this should not happen');
        this.paymentError = 'Payment form setup failed. Please try again.';
      }
    } catch (error) {
      console.error('❌ Error setting up payment elements:', error);
      this.paymentError = 'Failed to set up payment form. Please try again.';
    }
  }

  /**
   * Confirm payment with Stripe
   */
  async confirmPayment(): Promise<void> {
    if (!this.stripe || !this.elements || this.processing) {
      console.warn('⚠️ Cannot confirm payment - stripe:', !!this.stripe, 'elements:', !!this.elements, 'processing:', this.processing);
      return;
    }

    this.processing = true;
    this.paymentError = '';
    console.log('🔄 Starting payment confirmation...');
    console.log('📱 Stripe instance:', !!this.stripe);
    console.log('📝 Elements:', !!this.elements);

    try {
      console.log('🚀 Calling stripe.confirmPayment()...');

      // Run in zone to ensure change detection
      await this.ngZone.run(async () => {
        console.log('⏳ Inside ngZone.run()');

        const result = await this.stripe!.confirmPayment({
          elements: this.elements!,
          redirect: 'if_required',
          confirmParams: {
            return_url: `${window.location.origin}/billing?payment=success`
          }
        });

        console.log('📊 confirmPayment result:', result);

        if (result.error) {
          // Show error from Stripe
          console.error('❌ Payment error:', result.error);
          this.paymentError = result.error.message || 'Payment failed. Please try again.';
          this.processing = false;
        } else {
          // Payment successful
          console.log('✅ Payment confirmed!');
          this.showSuccessMessage = true;
          this.showProcessingMessage = true;  // Show "waiting..." message
          this.processing = false;

          // 🔄 Sync payment status and check invoice (MVP approach - simpler and more reliable)
          console.log('🔄 Syncing payment status with backend...');
          this.syncAndUpdateStatus();
        }
      });
    } catch (error) {
      console.error('❌ Error confirming payment:', error);
      this.paymentError = 'An unexpected error occurred. Please try again.';
      this.processing = false;
    }
  }

  /**
   * Sync payment status with Stripe and update database
   */
  private syncAndUpdateStatus(attempts: number = 0, maxAttempts: number = 5): void {
    if (attempts >= maxAttempts) {
      console.warn('⏱️ Sync attempts exhausted - falling back to polling');
      this.pollPaymentStatus();
      return;
    }

    console.log(`🔄 Syncing payment status... (attempt ${attempts + 1}/${maxAttempts})`);

    this.billingService.syncPaymentStatus(this.invoiceId).subscribe({
      next: (status) => {
        console.log('✅ Sync response - Status:', status.status);

        if (status.status === 'PAID') {
          // Database updated successfully!
          console.log('✅✅ Payment successfully synced and marked as PAID!');
          this.paymentSuccess.emit();
          setTimeout(() => {
            this.closeModal.emit();
          }, 1500);
        } else {
          // Still processing, retry sync or fall back to polling
          console.log('⏳ Payment still processing, retrying sync...');
          setTimeout(() => {
            this.syncAndUpdateStatus(attempts + 1, maxAttempts);
          }, 1000);
        }
      },
      error: (err) => {
        console.error('❌ Sync failed:', err);
        if (attempts < maxAttempts - 1) {
          // Retry sync
          setTimeout(() => {
            this.syncAndUpdateStatus(attempts + 1, maxAttempts);
          }, 1000);
        } else {
          // Max retries reached, fall back to polling
          console.warn('⚠️ Max sync retries reached, falling back to polling');
          this.pollPaymentStatus();
        }
      }
    });
  }

  /**
   * Poll payment status until it's marked as PAID or timeout
   * Fallback method if sync endpoint is unavailable
   */
  private pollPaymentStatus(attempts: number = 0, maxAttempts: number = 30): void {
    if (attempts >= maxAttempts) {
      console.warn('⏱️ Payment status poll timeout - closing modal');
      this.paymentSuccess.emit();
      this.closeModal.emit();
      return;
    }

    // Wait 1 second before polling
    setTimeout(() => {
      console.log(`🔍 Polling payment status... (attempt ${attempts + 1}/${maxAttempts})`);

      this.billingService.getPaymentStatus(this.invoiceId).subscribe({
        next: (status) => {
          console.log('📊 Payment status:', status.status);

          if (status.status === 'PAID') {
            // Payment successful!
            console.log('✅ Invoice marked as PAID!');
            this.clearTimeouts();
            this.paymentSuccess.emit();
            setTimeout(() => {
              this.closeModal.emit();
            }, 1500);
          } else {
            // Still pending, poll again
            this.pollPaymentStatus(attempts + 1, maxAttempts);
          }
        },
        error: (err) => {
          console.error('❌ Error checking payment status:', err);
          // Try again anyway
          this.pollPaymentStatus(attempts + 1, maxAttempts);
        }
      });
    }, 1000);
  }

  /**
   * FUTURE: WebSocket Listener for Real-Time Updates
   *
   * NOTE: MVP uses sync/polling approach instead. WebSocket can be added later
   * as an optimization when:
   * 1. Backend WebSocket endpoint is implemented
   * 2. Stripe CLI or actual webhooks are configured
   * 3. Real-time updates become critical for UX
   */
  private setupWebSocketListener(): void {
    console.log('🔌 WebSocket approach - deferred for future optimization');
    // For MVP: just use sync/polling (below)
  }

  /**
   * FUTURE: Countdown Timer for WebSocket
   */
  private startCountdownTimer(): void {
    // Deferred for WebSocket implementation
  }

  /**
   * Clear all timeout and interval handles
   */
  private clearTimeouts(): void {
    if (this.webhookTimeout) {
      clearTimeout(this.webhookTimeout);
      this.webhookTimeout = null;
    }
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
  }

  ngOnDestroy(): void {
    // Clean up timers
    this.clearTimeouts();
    if (this.webhookSocket) {
      this.webhookSocket.close();
    }

    // Clean up Stripe elements
    if (this.addressElement) {
      this.addressElement.destroy();
    }
    if (this.paymentElement) {
      this.paymentElement.destroy();
    }
    this.billingService.clearPaymentIntent();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
