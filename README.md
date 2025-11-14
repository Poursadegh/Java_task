# Microservices-Based Order & Payment System

A production-ready microservices architecture implementing an Order and Payment system using Spring Boot 3.2.0 and Spring Cloud.

## Architecture Overview

This system consists of three main components:

1. **Order Service** - Manages order lifecycle
2. **Payment Service** - Handles payment processing
3. **API Gateway** - Central entry point for all client requests

### Technology Stack

- **Spring Boot**: 3.2.0
- **Spring Cloud**: 2023.0.0
- **Java**: 17
- **Spring Cloud Gateway**: For API routing
- **OpenFeign**: For inter-service communication
- **JPA/Hibernate**: For data persistence
- **H2 Database**: For development (in-memory)
- **PostgreSQL**: For production
- **Docker**: For containerization
- **Maven**: For build management

## Project Structure

```
order-payment-system/
├── common/                 # Shared DTOs and common code
├── order-service/          # Order microservice
├── payment-service/        # Payment microservice
├── api-gateway/           # API Gateway service
├── docker-compose.yml     # Production Docker setup
├── docker-compose.dev.yml # Development Docker setup
└── pom.xml                # Parent POM
```

## Features

- ✅ Modular microservices architecture
- ✅ Inter-service communication using Feign Client
- ✅ Centralized API Gateway
- ✅ Environment-specific profiles (dev/prod)
- ✅ Docker containerization
- ✅ Production-ready configuration
- ✅ Health checks and monitoring (Actuator)
- ✅ Input validation
- ✅ Error handling
- ✅ Database per service pattern

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for containerized deployment)
- PostgreSQL (for production profile)

### Building the Project

```bash
# Build all modules
mvn clean install

# Build specific service
cd order-service && mvn clean install
cd payment-service && mvn clean install
cd api-gateway && mvn clean install
```

### Running Locally (Development)

1. **Start Order Service:**
```bash
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Runs on http://localhost:8081
```

2. **Start Payment Service:**
```bash
cd payment-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Runs on http://localhost:8082
```

3. **Start API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Runs on http://localhost:8080
```

### Running with Docker

**Development Mode (H2 in-memory database):**
```bash
docker-compose -f docker-compose.dev.yml up --build
```

**Production Mode (PostgreSQL):**
```bash
docker-compose up --build
```

This will start:
- Order Service on port 8081
- Payment Service on port 8082
- API Gateway on port 8080
- PostgreSQL databases (in production mode)

## API Endpoints

All requests should go through the API Gateway at `http://localhost:8080`

### Order Service Endpoints

- `POST /api/orders` - Create a new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders` - List all orders
- `PUT /api/orders/{id}/status` - Update order status

### Payment Service Endpoints

- `POST /api/payments` - Process a payment
- `GET /api/payments/{id}` - Get payment by ID
- `GET /api/payments` - List all payments
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

### Get Order Details

```bash
curl http://localhost:8080/api/orders/1
```

### Update Order Status

```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PROCESSING"
  }'
```

## Application Profiles

### Development Profile (`dev`)

- Uses H2 in-memory database
- Detailed logging (DEBUG level)
- H2 console enabled
- SQL queries logged

### Production Profile (`prod`)

- Uses PostgreSQL database
- Optimized logging (INFO level)
- Connection pooling configured
- File-based logging
- Environment variable configuration

### Setting Active Profile

```bash
# Via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Via Docker
docker run -e SPRING_PROFILES_ACTIVE=prod ...
```

## Configuration

### Environment Variables

**Order Service:**
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

**Payment Service:**
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `ORDER_SERVICE_URL` - Order service URL (default: http://localhost:8081)

## Health Checks

All services expose health endpoints via Spring Boot Actuator:

- Order Service: `http://localhost:8081/actuator/health`
- Payment Service: `http://localhost:8082/actuator/health`
- API Gateway: `http://localhost:8080/actuator/health`

## Inter-Service Communication

The Payment Service communicates with the Order Service using **OpenFeign**:

- When processing a payment, it validates the order exists
- After successful payment, it updates the order status to "PAID"
- Configured with timeout settings for resilience

## Database Schema

### Order Service
- `orders` table with fields: id, customer_id, amount, status, description, created_at, updated_at

### Payment Service
- `payments` table with fields: id, order_id, amount, payment_method, status, transaction_id, created_at, updated_at

## Production Deployment

1. Set up PostgreSQL databases
2. Configure environment variables
3. Build Docker images:
   ```bash
   mvn clean package
   docker-compose build
   ```
4. Deploy:
   ```bash
   docker-compose up -d
   ```

## Monitoring

- Actuator endpoints available at `/actuator`
- Health checks configured for all services
- Metrics available at `/actuator/metrics`

## Error Handling

- Global exception handlers in each service
- Validation errors return 400 Bad Request
- Not found errors return 404 Not Found
- Proper error messages in JSON format

## License

This project is for educational purposes.

"# Java_task" 
"# Java_task" 
