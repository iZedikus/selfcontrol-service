package ru.stepanov.selfcontrol.common;
import jakarta.persistence.*;import java.math.BigDecimal;
@Embeddable
public class Money { @Column(precision=19,scale=2) private BigDecimal amount; @Enumerated(EnumType.STRING) private CurrencyCode currency=CurrencyCode.RUB; public Money(){} public Money(BigDecimal amount,CurrencyCode currency){this.amount=amount;this.currency=currency;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;} public CurrencyCode getCurrency(){return currency;} public void setCurrency(CurrencyCode currency){this.currency=currency;} }
