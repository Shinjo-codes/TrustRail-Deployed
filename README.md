# TrustRail Backend - Complete Implementation Guide

## 🎯 What is TrustRail?

TrustRail is a **B2B trust orchestration platform** that sits between businesses (like CareFlow) and PayWithAccount (PWA). It answers the critical question:

> **"Should this customer be allowed to defer payment or use instalments?"**

### Core Value Proposition
- **PWA executes payments** (actual money movement)
- **TrustRail decides IF, WHEN, and HOW** payments should occur
- **Trust scoring** determines customer eligibility
- **Behavioral learning** improves decisions over time

---

## 🏗️ System Architecture

```
┌─────────────┐
│  CareFlow   │ (Business using TrustRail)
│  (Hospital) │
└──────┬──────┘
       │
       │ "Can patient get instalments?"
       ▼
┌─────────────────┐
│   TrustRail     │ (Trust Decision Engine)
│                 │
│ • Eligibility   │
│ • Trust Scoring │
│ • Policy Rules  │
└────────┬────────┘
         │
         │ "Execute debit for this mandate"
         ▼
┌──────────────────┐
│ PayWithAccount   │ (Payment Execution)
│                  │
│ • Account Linking│
│ • Mandate Creation│
│ • Direct Debits  │
└────────┬─────────┘
         │
         │ Webhooks (payment outcomes)
         ▼
    [Customer Bank Account]
```

---

## 📦 Project Structure

```
trustrail-backend/
├── src/main/java/com/trustrail/
│   ├── TrustRailApplication.java        # Main entry point
│   ├── model/
│   │   ├── entity/                       # Database models
│   │   │   ├── Business.java
│   │   │   ├── Customer.java
│   │   │   ├── TrustProfile.java        # ⭐ Core trust scoring
│   │   │   ├── Mandate.java
│   │   │   └── PaymentTransaction.java
│   │   └── dto/                          # API request/response objects
│   │       ├── EligibilityCheckRequest.java
│   │       ├── EligibilityCheckResponse.java
│   │       ├── MandateCreationRequest.java
│   │       └── ...
│   ├── repository/                       # Database access layer
│   ├── service/
│   │   ├── AuthService.java              # Login/registration
│   │   ├── TrustCalculationService.java  # ⭐ Trust score engine
│   │   ├── EligibilityService.java       # ⭐ Decision engine
│   │   ├── MandateService.java           # Mandate orchestration
│   │   ├── PaymentProcessingService.java # Scheduled debits
│   │   └── WebhookService.java           # PWA callbacks
│   ├── integration/pwa/
│   │   └── PWAIntegrationService.java    # ⭐ PWA API client
│   ├── security/
│   │   ├── JwtService.java               # JWT generation/validation
│   │   ├── JwtAuthenticationFilter.java  # Request interceptor
│   │   └── SecurityConfig.java           # Security rules
│   ├── controller/                       # REST API endpoints
│   │   ├── AuthController.java           # /auth/**
│   │   ├── EligibilityController.java    # /eligibility/**
│   │   ├── MandateController.java        # /mandates/**
│   │   └── WebhookController.java        # /webhooks/pwa
│   ├── exception/
│   │   └── GlobalExceptionHandler.java   # Error handling
│   └── config/
│       ├── WebConfig.java                # CORS configuration
│       └── DatabaseSeeder.java           # Sample data (dev only)
├── src/main/resources/
│   └── application.yml                   # Configuration
└── pom.xml                               # Maven dependencies
```

---

## 🚀 Quick Start (24-Hour Setup)

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Maven 3.8+
- IDE (IntelliJ IDEA / VS Code)

### Step 1: Database Setup (5 minutes)

```bash
# Create PostgreSQL database
createdb trustrail_db

# Or using psql
psql -U postgres
CREATE DATABASE trustrail_db;
\q
```

### Step 2: Configure Application (5 minutes)

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trustrail_db
    username: your_username
    password: your_password

jwt:
  secret: your-256-bit-secret-key-here-change-in-production

pwa:
  base-url: https://api.paywithaccount.com/v1
  api-key: YOUR_PWA_API_KEY
  merchant-id: YOUR_PWA_MERCHANT_ID
  webhook-secret: YOUR_WEBHOOK_SECRET
```

### Step 3: Build & Run (5 minutes)

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Server starts at http://localhost:8080
```

### Step 4: Test API (5 minutes)

```bash
# Register a business
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "CareFlow Healthcare",
    "email": "admin@careflow.com",
    "password": "password123",
    "phoneNumber": "+2348012345678",
    "businessType": "HEALTHCARE"
  }'

# Response includes JWT token
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "businessId": 1,
  "businessName": "CareFlow Healthcare",
  "status": "PENDING_VERIFICATION"
}
```

---

## 🔑 Key API Endpoints

### Authentication
```http
POST /api/auth/register        # Register new business
POST /api/auth/login           # Business login
```

### Customer Management
```http
POST /api/customers            # Create customer (patient)
GET  /api/customers            # List all customers
GET  /api/customers/{id}       # Get customer details
```

### Eligibility Check ⭐ **CRITICAL**
```http
POST /api/eligibility/check

Request:
{
  "externalCustomerId": "PATIENT-001",
  "amount": 50000.00,
  "paymentType": "INSTALMENT",
  "instalmentCount": 3,
  "isEmergency": false
}

Response:
{
  "eligible": true,
  "decision": "APPROVED",
  "reason": "Excellent trust score. Full amount approved.",
  "approvedAmount": 50000.00,
  "requiredDownPayment": 0.00,
  "maxInstalments": 4,
  "trustProfile": {
    "overallTrustScore": 80,
    "trustState": "TRUSTED"
  }
}
```

### Mandate Management
```http
POST /api/mandates             # Create mandate
GET  /api/mandates             # List all mandates
GET  /api/mandates/{ref}       # Get mandate details
```

### Payment Processing
```http
POST /api/payments/collect     # Manually trigger payment
```

### Webhooks (PWA → TrustRail)
```http
POST /api/webhooks/pwa         # Receive payment outcomes
```

### Dashboard
```http
GET /api/dashboard/stats       # Business analytics
```

---

## 🧠 Trust Calculation Logic

### Trust Score Components

1. **Behavioral Score (50% weight)**
   - Success rate: 40 points max
   - Consecutive failures penalty: -30 points max
   - Recent payment bonus: +20 points max

2. **Contextual Score (30% weight)**
   - Outstanding balance impact: -40 points max
   - No outstanding balance: +30 points bonus
   - Total amount paid bonus: +20 points max

3. **Progressive Score (20% weight)**
   - Account age: up to +30 points
   - Payment frequency: up to +20 points

### Trust States

| Score Range | State | Eligibility |
|------------|-------|-------------|
| 80-100 | TRUSTED | Full approval, no down payment |
| 60-79 | VERIFIED | 80% approved, 20% down payment |
| 40-59 | NEW | 50% down payment required |
| 20-39 | RESTRICTED | Emergency only |
| 0-19 | DEFAULTED | Denied |

### Example Calculation

```java
// Customer with:
// - 10 total payments, 9 successful, 1 failed
// - No consecutive failures
// - Last payment 15 days ago
// - ₦50,000 total paid
// - ₦10,000 outstanding
// - Account 120 days old

Behavioral Score:
  Success rate: (9/10) * 40 = 36
  Consecutive failures: 0 * 10 = 0
  Recent payment: 20 (within 30 days)
  = 56

Contextual Score:
  Outstanding ratio: 10k/100k = 10% → -0
  Total paid bonus: ₦50k → +10
  = 60

Progressive Score:
  Account age: 120 days → +20
  Payment frequency: 10 payments → +20
  = 90

Overall = (56 * 0.5) + (60 * 0.3) + (90 * 0.2)
        = 28 + 18 + 18
        = 64 → VERIFIED
```

---

## 🔗 PWA Integration Guide

### Step 1: Merchant Creation

When a business is approved, create them as a merchant on PWA:

```java
PWAMerchantRequest request = PWAMerchantRequest.builder()
    .businessName("CareFlow Healthcare")
    .email("admin@careflow.com")
    .phoneNumber("+2348012345678")
    .settlementAccountNumber("1234567890")
    .settlementBankCode("058")
    .settlementAccountName("CareFlow Healthcare Ltd")
    .firstName("Admin")
    .lastName("User")
    .build();

PWAMerchantResponse response = pwaService.createMerchant(request);
// Store response.merchantId in business.pwaBusinessId
```

### Step 2: Mandate Creation

After eligibility check passes, create mandate:

```java
PWAMandateRequest request = PWAMandateRequest.builder()
    .merchantId(business.getPwaBusinessId())
    .customerEmail(customer.getEmail())
    .customerPhone(customer.getPhoneNumber())
    .customerName(customer.getFirstName() + " " + customer.getLastName())
    .accountNumber(customer.getLinkedAccountNumber())
    .bankCode(customer.getLinkedBankCode())
    .maxAmount(mandate.getTotalAmount())
    .frequency("MONTHLY")
    .narration("Treatment payment plan")
    .build();

PWAMandateResponse response = pwaService.createMandate(request);
// Store response.mandateId and response.activationUrl
// Send activationUrl to customer
```

### Step 3: Instalment Invoice

After mandate is activated, send instalment invoice:

```java
PWAInstalmentRequest request = PWAInstalmentRequest.builder()
    .mandateId(mandate.getPwaMandateId())
    .totalAmount(mandate.getTotalAmount())
    .downPayment(mandate.getDownPayment())
    .instalmentCount(mandate.getInstalmentCount())
    .instalmentAmount(mandate.getInstalmentAmount())
    .repeatFrequency("MONTHLY")
    .repeatStartDate(mandate.getStartDate().toString())
    .narration("C-Section Surgery - 3 instalments")
    .build();

PWAInvoiceResponse response = pwaService.sendInstalmentInvoice(request);
// Response includes virtual account for down payment
```

### Step 4: Direct Debit (Scheduled or Manual)

When scheduled date arrives or manual collection triggered:

```java
PWACollectRequest request = PWACollectRequest.builder()
    .mandateId(mandate.getPwaMandateId())
    .amount(transaction.getAmount())
    .narration("Instalment 2 of 3")
    .build();

PWACollectResponse response = pwaService.collectPayment(request);
// Transaction initiated, webhook will confirm outcome
```

### Step 5: Webhook Handling

PWA sends webhooks after every payment attempt:

```json
// Payment Success
{
  "event": "payment.success",
  "transactionId": "PWA-TXN-12345",
  "mandateId": "PWA-MAN-67890",
  "status": "SUCCESSFUL",
  "amount": 16666.67,
  "timestamp": "2025-01-20T10:30:00Z"
}

// Payment Failure
{
  "event": "payment.failed",
  "transactionId": "PWA-TXN-12346",
  "mandateId": "PWA-MAN-67890",
  "status": "FAILED",
  "failureReason": "Insufficient funds",
  "timestamp": "2025-01-21T09:00:00Z"
}
```

TrustRail automatically:
- Updates transaction status
- Adjusts trust score
- Schedules retries if appropriate
- Notifies business

---

## 📊 Example Use Case: CareFlow Surgery Payment

### Scenario
Patient needs ₦50,000 brain surgery but can't pay upfront.

### Flow

1. **Eligibility Check**
   ```bash
   POST /api/eligibility/check
   {
     "externalCustomerId": "PATIENT-123",
     "amount": 50000,
     "paymentType": "INSTALMENT",
     "instalmentCount": 3
   }
   
   # TrustRail checks trust score (70/100)
   # Decision: CONDITIONAL - 20% down payment required
   ```

2. **Mandate Creation**
   ```bash
   POST /api/mandates
   {
     "externalCustomerId": "PATIENT-123",
     "mandateType": "INSTALMENT",
     "totalAmount": 50000,
     "instalmentCount": 3,
     "downPayment": 10000,
     "repeatFrequency": "MONTHLY"
   }
   
   # TrustRail creates mandate
   # Returns activation URL
   # Patient approves via PWA
   ```

3. **Payment Schedule Created**
   ```
   Down Payment: ₦10,000 (immediate via virtual account)
   Instalment 1: ₦13,333.33 (Month 1)
   Instalment 2: ₦13,333.33 (Month 2)
   Instalment 3: ₦13,333.34 (Month 3)
   ```

4. **Automated Debit Processing**
   ```
   Day 30: TrustRail checks trust score
          → Still VERIFIED? ✅
          → Calls PWA to debit ₦13,333.33
          → Success! Trust score increases
   
   Day 60: TrustRail checks trust score
          → Now TRUSTED (score 82)
          → Calls PWA to debit ₦13,333.33
          → Success!
   
   Day 90: Final instalment
          → Success!
          → Mandate COMPLETED
          → Trust score: 88
   ```

---

## 🔐 Security Features

### JWT Authentication
- Stateless tokens (24-hour expiration)
- Business ID embedded in token
- Auto-refresh on login

### Password Security
- BCrypt hashing (strength 10)
- Never stored in plain text

### API Authorization
- All endpoints require JWT (except /auth/**, /webhooks/**)
- Multi-tenancy enforced (businesses only see their data)

### Webhook Verification
- Signature validation (TODO: implement in production)
- Prevents spoofed callbacks

---

## 📈 Monitoring & Observability

### Logging
```java
log.info("Eligibility check for customer {} in business {}", customerId, businessId);
log.warn("Customer {} restricted due to {} consecutive failures", customerId, failures);
log.error("Failed to create PWA mandate", exception);
```

### Metrics to Track
- Average trust score
- Approval rate
- Payment success rate
- Customers at risk
- Outstanding balance

### Dashboard API
```bash
GET /api/dashboard/stats

Response:
{
  "totalCustomers": 150,
  "totalMandates": 75,
  "activeMandates": 45,
  "totalRevenue": 2500000.00,
  "outstandingBalance": 500000.00,
  "customersAtRisk": 8,
  "averageTrustScore": 68.5,
  "pendingPayments": 20
}
```

---

## 🧪 Testing

### Unit Tests (TODO)
```java
@Test
void testTrustScoreCalculation() {
    // Given
    TrustProfile profile = createProfile();
    
    // When
    trustCalculationService.recalculateTrustScore(customer);
    
    // Then
    assertEquals(66, profile.getOverallTrustScore());
    assertEquals(TrustState.VERIFIED, profile.getTrustState());
}
```

### Integration Tests (TODO)
```java
@Test
void testEligibilityCheckIntegration() {
    // Create customer with high trust
    // Check eligibility
    // Verify APPROVED decision
}
```

### Manual Testing with Postman
1. Import collection (TODO: create postman_collection.json)
2. Set environment variables (base_url, jwt_token)
3. Test complete flow: Register → Create Customer → Check Eligibility → Create Mandate

---

## 🚨 Troubleshooting

### Common Issues

**1. Database Connection Failed**
```
Error: Connection refused to PostgreSQL
Solution: Check application.yml credentials and ensure PostgreSQL is running
```

**2. JWT Token Invalid**
```
Error: 401 Unauthorized
Solution: Token expired (24h), login again to get new token
```

**3. PWA API Call Failed**
```
Error: PWA merchant creation failed
Solution: Check PWA API key in application.yml
```

**4. Webhook Not Received**
```
Issue: Payment completed but trust score not updated
Solution: Check webhook URL is publicly accessible (use ngrok for local dev)
```

---

## 📚 API Documentation

Full API documentation available at: `http://localhost:8080/swagger-ui.html` (TODO: Add SpringDoc OpenAPI)

---

## 🎓 Key Concepts for OnePipe Review

### 1. Separation of Concerns
- **CareFlow**: Business workflows
- **TrustRail**: Trust decisions
- **PWA**: Payment execution

### 2. Trust-Driven Decisions
- Not just "retry failed payment"
- Evaluate WHY payment failed
- Adjust eligibility based on behavior

### 3. Event-Driven Architecture
- Webhooks update trust scores
- Scheduled jobs check due payments
- Re-evaluation before every debit

### 4. Scalability
- Multi-tenant (multiple businesses)
- Stateless authentication (JWT)
- Async processing ready (WebClient)

### 5. Auditability
- Every transaction logged
- Payment history tracked
- Trust score changes recorded

---

## 🚀 Production Deployment Checklist

- [ ] Change `jwt.secret` to strong random value
- [ ] Set `ddl-auto` to `validate` (not `update`)
- [ ] Enable webhook signature verification
- [ ] Configure production database
- [ ] Set up application monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK stack)
- [ ] Set up CI/CD pipeline
- [ ] Enable HTTPS/TLS
- [ ] Configure rate limiting
- [ ] Remove DatabaseSeeder

---

## 📞 Support

For questions during OnePipe evaluation:
- Email: shina.ojo@trustrail.com
- GitHub: [trustrail/backend](https://github.com/trustrail/backend)

---

**Built with ❤️ for OnePipe Cortex 3.0**
