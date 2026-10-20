package com.example.Billing.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "database_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSequence {

    @Id
    private String id;

    private long seq;
}
