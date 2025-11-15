# Microservices-Based Order & Payment System

A production-ready microservices architecture implementing an Order and Payment system using Spring Boot 3.2.0, Spring Cloud, and Java 24.

## Architecture Overview

This system consists of three main components:

1. **Order Service** - Manages order lifecycle with partitioned database for millions of records
2. **Payment Service** - Handles payment processing with async capabilities
3. **API Gateway** - Central entry point for all client requests with rate limiting

### Technology Stack

- **Spring Boot**: 3.2.0
- **Spring Cloud**: 2023.0.0
- **Java**: 24
- **Spring Cloud Gateway**: For API routing
- **WebClient**: For reactive inter-service communication
- **Spring Cloud Consul**: For service discovery
- **JPA/Hibernate**: For data persistence
- **PostgreSQL**: For production database (with table partitioning)
- **Redis**: For distributed caching
- **Docker**: For containerization
- **Maven**: For build management
- **Lombok**: For reducing boilerplate code
- **Bucket4j**: For rate limiting
- **Jakarta Validation**: For comprehensive field validation
- **Resilience4j**: For Circuit Breaker and Retry patterns
- **RabbitMQ**: For event-driven messaging and async communication

## System Architecture

### Overview

This microservices-based system implements a distributed Order and Payment management system following microservices best practices. The architecture is designed to be scalable, maintainable, and production-ready with support for millions of orders through database partitioning.

### Architecture Diagram

```
                    ┌─────────────┐
                    │   Clients   │
                    └──────┬──────┘
                           │
                           │ HTTP/REST
                           │
                    ┌──────▼──────────┐
                    │   API Gateway   │
                    │  (Port 8080)    │
                    │  Rate Limiting  │
                    │  SSO Auth       │
                    └──────┬──────────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
    ┌───────▼──────┐ ┌─────▼──────┐ ┌────▼──────┐
    │Order Service │ │Payment     │ │  Other    │
    │(Port 8081)   │ │Service     │ │ Services  │
    │Partitioned DB│ │(Port 8082)  │ │           │
    └───────┬──────┘ └─────┬──────┘ └───────────┘
            │              │
            │              │ WebClient
            │              │ (Reactive HTTP)
            │              │ Circuit Breaker
            │              │ Retry
            │              │
            │      ┌───────▼──────┐
            │      │Order Service │
            │      │(for validation)│
            │      └──────────────┘
            │              │
            │              │ RabbitMQ
            │              │ (Events)
            │              │
            │      ┌───────▼──────┐
            │      │  RabbitMQ    │
            │      │  (Port 5672) │
            │      └──────────────┘
            │
    ┌───────▼──────┐ ┌───────▼──────┐ ┌───────▼──────┐
    │Order DB      │ │Payment DB    │ │   Redis     │
    │(PostgreSQL)  │ │(PostgreSQL)  │ │   Cache     │
    │Partitioned   │ │              │ │             │
    └──────────────┘ └──────────────┘ └──────────────┘
```

## Features

- ✅ Modular microservices architecture
- ✅ Reactive inter-service communication using WebClient
- ✅ Service discovery with Consul
- ✅ Centralized API Gateway
- ✅ Environment-specific profiles (dev/prod)
- ✅ Docker containerization
- ✅ Production-ready configuration
- ✅ Health checks and monitoring (Actuator)
- ✅ Comprehensive input validation (Jakarta Validation)
- ✅ Error handling with detailed messages
- ✅ Database per service pattern
- ✅ **PostgreSQL table partitioning for millions of orders**
- ✅ Redis distributed caching
- ✅ Virtual threads for high concurrency
- ✅ Rate limiting with Bucket4j
- ✅ Spring Security
- ✅ **Pagination support for orders and payments**
- ✅ **Field validations with best practices**
- ✅ **Circuit Breaker pattern with Resilience4j**
- ✅ **Retry mechanism with exponential backoff**
- ✅ **Event-driven communication with RabbitMQ**
- ✅ **WebClient timeout configuration for resilient communication**

## Getting Started

### Prerequisites

- **Java 24** JDK
- Maven 3.6+ (or use IntelliJ IDEA's built-in Maven)
- Docker and Docker Compose (for containerized deployment)
- PostgreSQL (for local development or use Docker)
- Redis (for caching, use Docker)
- RabbitMQ (for messaging, use Docker)

### Quick Start Options

#### Option 1: Docker Compose (Recommended)

**Using the setup script:**
```powershell
# Start all services in development mode
.\setup-docker.ps1 -Mode dev -Build

# Start all services in production mode
.\setup-docker.ps1 -Mode prod -Build

# Stop all services
.\setup-docker.ps1 -Stop -Mode dev

# Clean up everything
.\setup-docker.ps1 -Clean -Mode dev
```

**Manual Docker Compose:**
```bash
# Development mode
docker-compose -f docker-compose.dev.yml up --build

# Production mode
docker-compose up --build
```

This will start:
- Order Service on port 8081
- Payment Service on port 8082
- API Gateway on port 8080
- PostgreSQL databases
- Redis cache
- RabbitMQ message broker (port 5672, Management UI on 15672)

#### Option 2: IntelliJ IDEA (Recommended for Development)

**Pre-configured Run Configurations:**

The project includes IntelliJ run configurations in `.idea/runConfigurations/`:

1. **OrderServiceApplication** - Runs Order Service on port 8081
2. **PaymentServiceApplication** - Runs Payment Service on port 8082
3. **ApiGatewayApplication** - Runs API Gateway on port 8080

**To use:**

1. Open the project in IntelliJ IDEA
2. Go to Run → Edit Configurations
3. The configurations should be automatically detected
4. Run each service in order:
   - First: OrderServiceApplication
   - Second: PaymentServiceApplication
   - Third: ApiGatewayApplication

**Or use the Run menu:**
- Run → Run 'OrderServiceApplication'
- Run → Run 'PaymentServiceApplication'
- Run → Run 'ApiGatewayApplication'

**Environment Variables (already configured):**
- Order Service: Uses `appuser/123456` for database, connects to `localhost:5432/orderdb`
- Payment Service: Uses default PostgreSQL credentials, connects to `localhost:5434/paymentdb`
- All services: Redis on `localhost:6379`

#### Option 3: Maven Command Line

**Build the project:**
```bash
# Build all modules
mvn clean install -DskipTests

# Note: If using Java 24, you may need to use IntelliJ IDEA's Maven
# or ensure Lombok compatibility
```

**Run services:**
```bash
# Terminal 1: Order Service
cd order-service
mvn spring-boot:run

# Terminal 2: Payment Service
cd payment-service
mvn spring-boot:run

# Terminal 3: API Gateway
cd api-gateway
mvn spring-boot:run
```

## Database Setup

### PostgreSQL Partitioning for Orders

The Order Service uses **PostgreSQL table partitioning** to handle millions of orders efficiently.

#### Setup Partitioning

**Using Docker container:**
```powershell
# Connect to PostgreSQL container
docker exec -it comms-postgres psql -U appuser -d orderdb

# Run migration scripts
\i order-service/src/main/resources/db/migration/V1__create_orders_partitioned_table.sql
\i order-service/src/main/resources/db/migration/V2__migrate_existing_orders_to_partitioned.sql
\i order-service/src/main/resources/db/migration/V3__create_partition_maintenance_job.sql
\i order-service/src/main/resources/db/migration/V4__create_orders_view_with_triggers.sql
\i order-service/src/main/resources/db/migration/V5__setup_partition_maintenance_schedule.sql
```

**Or use the setup script:**
```powershell
.\order-service\setup-partitioning.ps1
```

#### Partitioning Architecture

- **Strategy**: Range partitioning by month based on `created_at` timestamp
- **Granularity**: Monthly partitions
- **Benefits**:
  - Improved query performance (partition pruning)
  - Easier data management (archive/delete old partitions)
  - Better index maintenance
  - Parallel query execution across partitions

#### Partition Maintenance

**Automatic maintenance:**
```sql
-- Create partitions for next 3 months, keep last 24 months
SELECT run_partition_maintenance();
```

**Schedule monthly maintenance:**

**Windows Task Scheduler:**
1. Open Task Scheduler
2. Create Basic Task
3. Trigger: Monthly, 1st day at 2:00 AM
4. Action: Start a program
5. Program: `powershell.exe`
6. Arguments: `-File "C:\path\to\order-service\maintain-partitions.ps1"`

**Linux/Mac cron:**
```bash
# Add to crontab
0 2 1 * * /path/to/order-service/maintain-partitions.sh
```

**Manual maintenance:**
```powershell
# Run maintenance script
.\order-service\maintain-partitions.ps1
```

#### Verify Partitioning

```sql
-- List all partitions
SELECT tablename FROM pg_tables WHERE tablename LIKE 'orders_partitioned_%';

-- Check partition sizes
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(tablename)) AS size
FROM pg_tables
WHERE tablename LIKE 'orders_partitioned_%'
ORDER BY tablename;
```

## API Endpoints

All requests should go through the API Gateway at `http://localhost:8080`

### Order Service Endpoints

- `POST /api/orders` - Create a new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders?page=0&size=10&sort=createdAt,desc` - List orders with pagination
- `PUT /api/orders/{id}/status` - Update order status

### Payment Service Endpoints

- `POST /api/payments` - Process a payment
- `GET /api/payments/{id}` - Get payment by ID
- `GET /api/payments?page=0&size=10&sort=createdAt,desc` - List payments with pagination
- `PUT /api/payments/{id}/status` - Update payment status

### Direct Service Access (for testing)

- Order Service: `http://localhost:8081/orders`
- Payment Service: `http://localhost:8082/payments`

## API Examples

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "amount": 99.99,
    "description": "Sample order"
  }'
```

### Process a Payment

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 99.99,
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Get Orders with Pagination

```bash
# Get first page (10 items)
curl "http://localhost:8080/api/orders?page=0&size=10&sort=createdAt,desc"

# Get second page
curl "http://localhost:8080/api/orders?page=1&size=10&sort=createdAt,desc"
```

### Get Payments with Pagination

```bash
# Get first page (10 items)
curl "http://localhost:8080/api/payments?page=0&size=10&sort=createdAt,desc"
```

## Field Validations

The system implements comprehensive field validations using Jakarta Validation:

### OrderDTO Validations

- `customerId`: Required, 1-100 characters, alphanumeric with hyphens/underscores
- `amount`: Required, between 0.01 and 999,999.99
- `status`: Must be one of: PENDING, PAID, CANCELLED, PROCESSING
- `description`: Maximum 500 characters

### PaymentDTO Validations

- `orderId`: Required, positive number
- `amount`: Required, between 0.01 and 999,999.99
- `paymentMethod`: Required, must be: CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CASH, OTHER
- `status`: Must be one of: PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED
- `transactionId`: Maximum 100 characters, alphanumeric with hyphens/underscores

### Path Variable Validations

- All ID path variables validated as positive numbers
- Validation errors return 400 Bad Request with detailed messages

## Pagination

Both Order and Payment services support pagination:

### Default Pagination

- **Page size**: 10 items per page
- **Sort**: By `createdAt` descending (newest first)

### Custom Pagination

```bash
# Custom page size and sorting
GET /api/orders?page=0&size=20&sort=amount,desc
GET /api/orders?page=0&size=5&sort=customerId,asc&sort=createdAt,desc
```

### Pagination Response Format

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {...}
  },
  "totalElements": 150,
  "totalPages": 15,
  "first": true,
  "last": false
}
```

## Service Details

### Order Service

**Responsibilities:**
- Create and manage orders
- Track order status
- Store order information in partitioned database

**Endpoints:**
- `POST /orders` - Create order
- `GET /orders/{id}` - Get order
- `GET /orders` - List orders (paginated)
- `PUT /orders/{id}/status` - Update status

**Database**: `orderdb` (PostgreSQL with monthly partitioning)

**Status Values**: PENDING, PAID, CANCELLED, PROCESSING

**Features:**
- Redis caching for read operations
- Virtual threads enabled
- Spring Security configured
- **Table partitioning for scalability**
- **Pagination support**
- **Comprehensive field validation**

### Payment Service

**Responsibilities:**
- Process payments
- Validate orders (via Order Service)
- Update order status after payment
- Track payment transactions

**Endpoints:**
- `POST /payments` - Process payment
- `GET /payments/{id}` - Get payment
- `GET /payments` - List payments (paginated)
- `PUT /payments/{id}/status` - Update status

**Database**: `paymentdb` (PostgreSQL)

**Status Values**: PENDING, COMPLETED, FAILED, REFUNDED

**Inter-Service Calls:**
- Validates order exists before processing using WebClient
- Updates order status to "PAID" after successful payment
- Reactive communication with load balancing
- Circuit Breaker protection for order service calls
- Automatic retry with exponential backoff
- Event-driven communication via RabbitMQ

**Features:**
- Virtual threads enabled
- Spring Security configured
- **Pagination support**
- **Comprehensive field validation**
- Async payment processing

### API Gateway

**Responsibilities:**
- Route requests to appropriate services
- Handle CORS
- Provide single entry point
- Rate limiting

**Routes:**
- `/api/orders/**` → Order Service (port 8081)
- `/api/payments/**` → Payment Service (port 8082)

**Features:**
- Path-based routing
- Strip prefix filter
- CORS configuration
- Rate limiting with Bucket4j (configurable capacity and refill)
- Virtual threads enabled
- Spring Security configured
- SSO authentication filter
- Automatic cache cleanup for rate limiting

## Configuration

### Environment Variables

**Order Service:**
- `DB_URL` - Database connection URL (default: `jdbc:postgresql://localhost:5432/orderdb`)
- `DB_USERNAME` - Database username (default: `appuser`)
- `DB_PASSWORD` - Database password (default: `123456`)
- `REDIS_HOST` - Redis host (default: `localhost`)
- `REDIS_PORT` - Redis port (default: `6379`)

**Payment Service:**
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `ORDER_SERVICE_URL` - Order service URL (default: `http://localhost:8081`)
- `REDIS_HOST` - Redis host (default: `localhost`)
- `REDIS_PORT` - Redis port (default: `6379`)
- `RABBITMQ_HOST` - RabbitMQ host (default: `localhost`)
- `RABBITMQ_PORT` - RabbitMQ port (default: `5672`)
- `RABBITMQ_USERNAME` - RabbitMQ username (default: `guest`)
- `RABBITMQ_PASSWORD` - RabbitMQ password (default: `guest`)

**API Gateway:**
- `ORDER_SERVICE_URL` - Order service URL (default: `http://localhost:8081`)
- `PAYMENT_SERVICE_URL` - Payment service URL (default: `http://localhost:8082`)
- `RATE_LIMIT_CAPACITY` - Rate limit capacity (default: `100`)
- `RATE_LIMIT_REFILL_TOKENS` - Tokens to refill (default: `100`)
- `RATE_LIMIT_REFILL_DURATION_MINUTES` - Refill duration in minutes (default: `1`)
- `RATE_LIMIT_CACHE_CLEANUP_INTERVAL_MINUTES` - Cache cleanup interval (default: `60`)

**Order Service:**
- `RABBITMQ_HOST` - RabbitMQ host (default: `localhost`)
- `RABBITMQ_PORT` - RabbitMQ port (default: `5672`)
- `RABBITMQ_USERNAME` - RabbitMQ username (default: `guest`)
- `RABBITMQ_PASSWORD` - RabbitMQ password (default: `guest`)

### Application Profiles

**Development Profile (`dev`):**
- Detailed logging (DEBUG level)
- SQL queries logged
- CORS enabled for all origins
- Database: `localhost:5432/orderdb` (appuser/123456)

**Production Profile (`prod`):**
- Optimized logging (INFO level)
- Connection pooling configured
- File-based logging
- Environment variable configuration

## Database Partitioning Details

### Architecture

- **Partitioning Strategy**: Range partitioning by month based on `created_at` timestamp
- **Partition Granularity**: Monthly partitions
- **Primary Key**: Composite `(id, created_at)` for PostgreSQL compatibility
- **JPA Compatibility**: View with INSTEAD OF triggers for seamless integration

### Benefits

- **Query Performance**: Partition pruning automatically filters irrelevant partitions
- **Data Management**: Easy archive/delete of old partitions
- **Index Maintenance**: Smaller indexes per partition
- **Parallel Execution**: PostgreSQL can query partitions in parallel

### Maintenance

**Automatic Functions:**
- `create_future_partitions()` - Creates partitions for future months
- `drop_old_partitions()` - Archives/drops old partitions
- `maintain_order_partitions()` - Combined maintenance function
- `run_partition_maintenance()` - Scheduled maintenance wrapper

**Best Practices:**
1. Always include `created_at` in WHERE clauses when possible
2. Use date ranges for time-based queries
3. Monitor partition sizes (keep under 10-50 million rows)
4. Archive old data before dropping partitions
5. Schedule monthly maintenance

### Query Optimization

**Efficient (uses partition pruning):**
```sql
SELECT * FROM orders_partitioned 
WHERE created_at >= '2024-01-01' AND created_at < '2024-02-01';
```

**Less Efficient (scans all partitions):**
```sql
SELECT * FROM orders_partitioned WHERE customer_id = 'CUST123';
```

## Health Checks

All services expose health endpoints via Spring Boot Actuator:

- Order Service: `http://localhost:8081/actuator/health`
- Payment Service: `http://localhost:8082/actuator/health`
- API Gateway: `http://localhost:8080/actuator/health`

## Error Handling

- Global exception handlers in each service
- Validation errors return 400 Bad Request with field-level details
- Not found errors return 404 Not Found
- Constraint violations handled with detailed messages
- Proper error messages in JSON format

## Resilience and Communication Patterns

### Circuit Breaker (Resilience4j)

The system implements Circuit Breaker pattern to prevent cascade failures:

- **Configuration**: Failure rate threshold of 50%
- **Sliding Window**: 10 requests
- **Open State Duration**: 10 seconds
- **Half-Open State**: Automatic transition with 3 permitted calls
- **Fallback Methods**: Graceful degradation when service is unavailable

**Applied to:**
- Payment Service → Order Service communication
- All inter-service calls via WebClient

### Retry Mechanism

- **Max Attempts**: 3 retries
- **Wait Duration**: 1 second initial delay
- **Exponential Backoff**: Enabled with multiplier of 2
- **Applied to**: All inter-service HTTP calls

### Event-Driven Communication (RabbitMQ)

The system uses RabbitMQ for asynchronous event-driven communication:

**Events Published:**
- `OrderCreatedEvent` - When a new order is created
- `OrderStatusUpdatedEvent` - When order status changes
- `PaymentProcessedEvent` - When payment is processed (success or failure)

**Event Listeners:**
- Payment Service listens to order events
- Enables loose coupling between services
- Supports eventual consistency

**RabbitMQ Management UI:**
- Access at `http://localhost:15672`
- Default credentials: `guest/guest`

### WebClient Timeout Configuration

- **Connect Timeout**: 5 seconds
- **Read Timeout**: 10 seconds
- **Write Timeout**: 10 seconds
- **Response Timeout**: 10 seconds

## Performance Optimization

### Current Optimizations

- Connection pooling (HikariCP)
- Efficient queries (JPA with pagination)
- Proper indexing on partitioned tables
- Redis distributed caching
- Virtual threads for high concurrency
- Database partitioning for millions of records
- Circuit Breaker to prevent cascade failures
- Retry mechanism for transient failures
- Event-driven architecture for async processing

### Scalability

- **Horizontal Scaling**: Each service can be scaled independently
- **Database Scaling**: Partitioned tables support millions of records
- **Caching**: Redis reduces database load significantly
- **Virtual Threads**: Support for millions of concurrent requests
- **Message Queue**: RabbitMQ enables async processing and decoupling
- **Circuit Breaker**: Prevents overload during high traffic

## Troubleshooting

### Build Issues with Java 24

If you encounter Lombok compilation issues with Java 24:

1. **Use IntelliJ IDEA** - Handles Lombok annotation processing automatically
2. **Use Docker** - Docker images use compatible Java versions
3. **Use Java 21** - Temporarily switch if needed

### Database Connection Issues

**Check PostgreSQL is running:**
```bash
docker ps | grep postgres
```

**Verify connection:**
```bash
docker exec comms-postgres psql -U appuser -d orderdb -c "SELECT 1;"
```

### Partition Missing Errors

**Create missing partition:**
```sql
SELECT create_monthly_partition('orders_partitioned', '2024-01-01'::DATE);
```

**Or run maintenance:**
```sql
SELECT run_partition_maintenance();
```

## Project Structure

```
order-payment-system/
├── common/                          # Shared DTOs and common code
│   └── src/main/java/com/microservices/common/
│       ├── dto/                     # Data Transfer Objects
│       ├── enums/                   # Enumerations
│       ├── exception/               # Custom exceptions
│       └── event/                    # Event classes
│           ├── OrderCreatedEvent.java
│           ├── OrderStatusUpdatedEvent.java
│           └── PaymentProcessedEvent.java
├── order-service/                   # Order microservice
│   ├── src/main/java/com/microservices/orderservice/
│   │   ├── config/
│   │   │   └── RabbitMQConfig.java  # RabbitMQ configuration
│   │   ├── messaging/
│   │   │   └── OrderEventPublisher.java  # Event publisher
│   │   └── service/
│   │       └── OrderService.java    # Publishes events
│   ├── src/main/resources/
│   │   └── db/migration/           # Database migration scripts
│   │       ├── V1__create_orders_partitioned_table.sql
│   │       ├── V2__migrate_existing_orders_to_partitioned.sql
│   │       ├── V3__create_partition_maintenance_job.sql
│   │       ├── V4__create_orders_view_with_triggers.sql
│   │       └── V5__setup_partition_maintenance_schedule.sql
│   ├── maintain-partitions.ps1     # Partition maintenance script
│   └── setup-partitioning.ps1      # Partitioning setup script
├── payment-service/                # Payment microservice
│   ├── src/main/java/com/microservices/paymentservice/
│   │   ├── config/
│   │   │   ├── RabbitMQConfig.java  # RabbitMQ configuration
│   │   │   └── WebClientConfig.java # WebClient with timeout
│   │   ├── messaging/
│   │   │   ├── PaymentEventPublisher.java  # Event publisher
│   │   │   └── OrderEventListener.java    # Event listener
│   │   └── service/
│   │       ├── PaymentService.java  # Publishes events
│   │       └── OrderServiceClient.java  # With Circuit Breaker
│   └── src/main/resources/
│       └── application.properties   # Resilience4j config
├── api-gateway/                     # API Gateway service
│   ├── src/main/java/com/microservices/apigateway/
│   │   └── filter/
│   │       ├── RateLimitFilter.java  # Enhanced rate limiting
│   │       └── SsoAuthFilter.java    # SSO authentication
│   └── src/main/resources/
│       └── application.properties   # Rate limit config
├── .idea/runConfigurations/         # IntelliJ run configurations
│   ├── OrderServiceApplication.xml
│   ├── PaymentServiceApplication.xml
│   └── ApiGatewayApplication.xml
├── docker-compose.yml               # Production Docker setup
├── docker-compose.dev.yml           # Development Docker setup
├── setup-docker.ps1                 # Docker setup script
└── pom.xml                          # Parent POM
```

## Development Workflow

### Using IntelliJ IDEA

1. **Open Project**: Open the root directory in IntelliJ IDEA
2. **Build**: Build → Build Project (Ctrl+F9)
3. **Run Services**: Use the pre-configured run configurations
4. **Debug**: Set breakpoints and use Debug mode

### Using Docker

1. **Start Infrastructure**: Ensure PostgreSQL and Redis are running
2. **Setup Partitioning**: Run partitioning scripts if needed
3. **Start Services**: Use `setup-docker.ps1` or `docker-compose up`
4. **View Logs**: `docker-compose logs -f [service-name]`

## Production Deployment

1. **Set up PostgreSQL databases** with partitioning
2. **Set up Redis cache**
3. **Set up RabbitMQ message broker**
4. **Configure environment variables** (see Configuration section)
5. **Build Docker images**:
   ```bash
   mvn clean package
   docker-compose build
   ```
6. **Deploy**:
   ```bash
   docker-compose up -d
   ```
7. **Verify services**:
   - Check health endpoints: `/actuator/health`
   - Access RabbitMQ Management UI: `http://localhost:15672`
   - Monitor circuit breaker metrics
8. **Schedule partition maintenance** (monthly)

## Monitoring

- Actuator endpoints available at `/actuator`
- Health checks configured for all services
- Metrics available at `/actuator/metrics`
- Partition monitoring via SQL queries
- RabbitMQ Management UI at `http://localhost:15672`
- Circuit Breaker metrics via Resilience4j
- Rate limit metrics in API Gateway

## Security Considerations

### Current Implementation

- Input validation with Jakarta Validation
- SQL injection prevention (JPA)
- Error message sanitization
- Spring Security configured
- Rate limiting with Bucket4j (configurable per IP)
- CSRF disabled for stateless API
- Path variable validation
- SSO authentication filter in API Gateway
- Token-based authentication between services

### Future Enhancements

- Full JWT/OAuth2 implementation
- Enhanced role-based access control
- HTTPS/TLS
- Secrets management (Vault)
- Distributed tracing (Zipkin/Jaeger)
- Advanced monitoring (Prometheus/Grafana)

## License

This project is for educational purposes.
