import {
  Component,
  input,
  output,
  ViewChild,
  ElementRef,
  OnInit,
  OnDestroy,
  NgZone,
  ChangeDetectionStrategy,
  inject,
  DestroyRef,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { BillingService } from '../../billing.service';
import { timer, EMPTY } from 'rxjs';
import { switchMap, take, filter, catchError } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { loadStripe, Stripe, StripeElements, StripePaymentElement, StripeAddressElement } from '@stripe/stripe-js';

@Component({
  selector: 'app-stripe-payment-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  readonly invoiceId = input.required<string>();
  readonly closeModal = output<void>();
  readonly paymentSuccess = output<void>();

  @ViewChild('stripeContainer') stripeContainerRef!: ElementRef;
  @ViewChild('addressContainer') addressContainerRef!: ElementRef;

  private readonly billingService = inject(BillingService);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);

  readonly paymentIntent = this.billingService.paymentIntent;

  stripe: Stripe | null = null;
  elements: StripeElements | null = null;
  paymentElement: StripePaymentElement | null = null;
  addressElement: StripeAddressElement | null = null;

  readonly stripeReadySignal = signal<boolean>(false);
  readonly processingSignal = signal<boolean>(false);
  readonly paymentErrorSignal = signal<string>('');
  readonly showSuccessMessageSignal = signal<boolean>(false);
  readonly showProcessingMessageSignal = signal<boolean>(false);
  readonly timeoutCountdownSignal = signal<number>(300);

  private webhookSocket: WebSocket | null = null;
  private webhookTimeout: ReturnType<typeof setTimeout> | null = null;
  private countdownInterval: ReturnType<typeof setInterval> | null = null;

  get stripeReady(): boolean {
    return this.stripeReadySignal();
  }
  set stripeReady(val: boolean) {
    this.stripeReadySignal.set(val);
  }

  get processing(): boolean {
    return this.processingSignal();
  }
  set processing(val: boolean) {
    this.processingSignal.set(val);
  }

  get paymentError(): string {
    return this.paymentErrorSignal();
  }
  set paymentError(val: string) {
    this.paymentErrorSignal.set(val);
  }

  get showSuccessMessage(): boolean {
    return this.showSuccessMessageSignal();
  }
  set showSuccessMessage(val: boolean) {
    this.showSuccessMessageSignal.set(val);
  }

  get showProcessingMessage(): boolean {
    return this.showProcessingMessageSignal();
  }
  set showProcessingMessage(val: boolean) {
    this.showProcessingMessageSignal.set(val);
  }

  get timeoutCountdown(): number {
    return this.timeoutCountdownSignal();
  }
  set timeoutCountdown(val: number) {
    this.timeoutCountdownSignal.set(val);
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
      const targetInvoiceId = this.invoiceId();
      console.log('💳 Creating payment intent for invoice:', targetInvoiceId);
      this.billingService
        .createPaymentIntent(targetInvoiceId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (response) => {
            console.log('✅ Payment intent created:', response);
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
        mode: 'billing',
        defaultValues: {
          address: {
            country: 'IN'
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
      this.paymentElement = this.elements.create('payment');
      console.log('✅ Payment element created with auto billing details collection');

      // Mount payment element
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
          console.error('❌ Payment error:', result.error);
          this.paymentError = result.error.message || 'Payment failed. Please try again.';
          this.processing = false;
        } else {
          console.log('✅ Payment confirmed!');
          this.showSuccessMessage = true;
          this.showProcessingMessage = true;
          this.processing = false;

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
  private syncAndUpdateStatus(maxAttempts: number = 5): void {
    console.log('🔄 Syncing payment status with backend...');
    const targetInvoiceId = this.invoiceId();
    timer(0, 1000).pipe(
      take(maxAttempts),
      takeUntilDestroyed(this.destroyRef),
      switchMap((attempt) => {
        console.log(`🔄 Syncing payment status... (attempt ${attempt + 1}/${maxAttempts})`);
        return this.billingService.syncPaymentStatus(targetInvoiceId).pipe(
          catchError((err) => {
            console.error('❌ Sync failed:', err);
            return EMPTY;
          })
        );
      }),
      filter(status => status?.status === 'PAID'),
      take(1)
    ).subscribe({
      next: () => {
        console.log('✅✅ Payment successfully synced and marked as PAID!');
        this.clearTimeouts();
        this.paymentSuccess.emit();
        setTimeout(() => {
          this.closeModal.emit();
        }, 1500);
      },
      complete: () => {
        console.warn('⚠️ Sync attempts completed without PAID, falling back to polling');
        this.pollPaymentStatus();
      }
    });
  }

  /**
   * Poll payment status until it's marked as PAID or timeout
   */
  private pollPaymentStatus(maxAttempts: number = 30): void {
    console.log('🔍 Starting declarative polling for payment status...');
    const targetInvoiceId = this.invoiceId();
    timer(0, 1000).pipe(
      take(maxAttempts),
      takeUntilDestroyed(this.destroyRef),
      switchMap((attempt) => {
        console.log(`🔍 Polling payment status... (attempt ${attempt + 1}/${maxAttempts})`);
        return this.billingService.getPaymentStatus(targetInvoiceId).pipe(
          catchError((err) => {
            console.error('❌ Error checking payment status:', err);
            return EMPTY;
          })
        );
      }),
      filter(status => status?.status === 'PAID'),
      take(1)
    ).subscribe({
      next: () => {
        console.log('✅ Invoice marked as PAID!');
        this.clearTimeouts();
        this.paymentSuccess.emit();
        setTimeout(() => {
          this.closeModal.emit();
        }, 1500);
      },
      complete: () => {
        console.warn('⏱️ Payment status poll timeout - closing modal');
        this.paymentSuccess.emit();
        this.closeModal.emit();
      }
    });
  }

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
    this.clearTimeouts();
    if (this.webhookSocket) {
      this.webhookSocket.close();
    }

    if (this.addressElement) {
      this.addressElement.destroy();
    }
    if (this.paymentElement) {
      this.paymentElement.destroy();
    }
    this.billingService.clearPaymentIntent();
  }
}
