# ShopEase Backend API

ShopEase is a full-stack e-commerce project built with a Spring Boot backend and an integrated browser UI served from the same application.

The application lets customers browse products, add items to a cart, place orders, and view their order history. Admin users can manage categories, products, and order status from the same browser interface.

## Project at a glance

| Item | Details |
|---|---|
| Project type | E-commerce backend with integrated frontend UI |
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA |
| Frontend | HTML, CSS, JavaScript served from Spring Boot |
| Authentication | JWT bearer token |
| Roles | `USER`, `ADMIN` |
| Local database | H2 in-memory database |
| Optional database | MySQL using Docker Compose |
| API docs | Swagger/OpenAPI |
| Deployment | Docker + Render Blueprint |
| Repository | `Dinesh12328/shopease-backend-api` |

## Main features

### User features

- Register a new user
- Login and receive JWT token
- Browse product catalog
- Search products by name, brand, and category
- Sort products by price, name, and newest
- Add products to cart
- Update cart item quantity
- Remove cart items
- Place orders
- View order history

### Admin features

- Login as admin
- Create product categories
- Create products
- Delete products
- Upload product image URL
- View all orders
- Update order status

### Frontend UI features

- Integrated shopping UI at `/`
- Dashboard counters for products, categories, cart items, and orders
- Login/register forms
- Product catalog cards
- Search, filter, and sorting controls
- Cart drawer
- Checkout form
- Order history cards
- Admin studio
- Toast success/error messages
- Responsive layout for desktop and mobile

## Tech stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- H2 database
- MySQL
- Maven
- Swagger/OpenAPI
- Docker
- Docker Compose
- Render deployment config
- HTML, CSS, JavaScript

## Project architecture

```text
Browser / Frontend / Swagger / Postman
              |
              v
        Controller layer
              |
              v
         Service layer
              |
              v
       Repository layer
              |
              v
          Database
```

### Package structure

```text
src/main/java/com/shopease
  config       -> security, Swagger, web config, default admin setup
  controller   -> REST API controllers
  dto          -> request and response DTOs
  entity       -> JPA entities
  exception    -> global exception handling
  repository   -> Spring Data JPA repositories
  security     -> JWT service and JWT filter
  service      -> business logic

src/main/resources
  application.yml
  static/index.html
  static/styles.css
  static/app.js

src/test
  integration tests

Dockerfile
docker-compose.yml
render.yaml
pom.xml
README.md
```

## Important entities

- `User`
- `Product`
- `Category`
- `Cart`
- `CartItem`
- `Order`
- `OrderItem`
- `Payment`

### Entity relationships

```text
User 1 ---- 1 Cart
Cart 1 ---- * CartItem
Product * ---- 1 Category
User 1 ---- * Order
Order 1 ---- * OrderItem
Order 1 ---- 1 Payment
```

## Default local admin account

When the application starts, it creates a default admin user if one does not already exist.

```text
Email:    admin@shopease.com
Password: Admin@123
```

Use this only for local development. For online deployment, set a private admin password in the deployment environment.

## Requirements

Install these before running locally:

1. Java 17 or newer
2. IntelliJ IDEA
3. Maven

Optional:

4. Docker Desktop
5. Postman

## Run locally with IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Click `File` -> `Open`.
3. Select the project folder:

   ```text
   1-shopease-backend-api-project-type
   ```

4. Wait for Maven dependencies to load.
5. Make sure Java 17 or newer is selected:

   ```text
   File -> Project Structure -> Project SDK
   ```

6. Open:

   ```text
   src/main/java/com/shopease/ShopEaseApplication.java
   ```

7. Click the green run button.
8. Wait for this message:

   ```text
   Started ShopEaseApplication
   ```

9. Open the frontend:

   ```text
   http://127.0.0.1:8080/
   ```

## Run locally from terminal

From the project root:

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package
java -jar target/shopease-api-1.0.0.jar
```

## Important local URLs

| Purpose | URL |
|---|---|
| Frontend UI | `http://127.0.0.1:8080/` |
| Swagger UI | `http://127.0.0.1:8080/swagger-ui/index.html` |
| Health check | `http://127.0.0.1:8080/actuator/health` |
| OpenAPI JSON | `http://127.0.0.1:8080/v3/api-docs` |
| H2 console | `http://127.0.0.1:8080/h2-console` |

Health check expected response:

```json
{
  "status": "UP"
}
```

## H2 database login

Open:

```text
http://127.0.0.1:8080/h2-console
```

Use:

```text
JDBC URL: jdbc:h2:mem:shopease
Username: sa
Password:
```

Leave password empty.

Important: H2 is an in-memory database. Data resets when the application stops.

## How to test the frontend UI

Open:

```text
http://127.0.0.1:8080/
```

### Full frontend testing flow

1. Login as admin:

   ```text
   admin@shopease.com
   Admin@123
   ```

2. Go to the admin section.
3. Create a category, for example:

   ```text
   Electronics
   ```

4. Create a product under that category.
   - In `Image URL`, paste a public browser image link.
   - Links like `https://example.com/product.jpg`, `www.example.com/product.png`, and long image links with query text are accepted.
   - If an outside website blocks image loading, the product card keeps its layout and shows a fallback instead of breaking.
5. Go to the catalog section.
6. Search/filter/sort products.
7. Register a normal user.
8. Add product to cart.
9. Open the cart drawer.
10. Update quantity or remove item.
11. Place order.
12. View order history.
13. Login again as admin.
14. View all orders.
15. Update order status.

## How to test using Swagger

Open:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

### Step 1: Login as admin

Endpoint:

```text
POST /api/auth/login
```

Body:

```json
{
  "email": "admin@shopease.com",
  "password": "Admin@123"
}
```

Copy:

```text
data.token
```

### Step 2: Authorize Swagger

Click `Authorize` in Swagger.

Paste:

```text
Bearer YOUR_TOKEN_HERE
```

### Step 3: Create category

Endpoint:

```text
POST /api/admin/categories
```

Body:

```json
{
  "name": "Electronics",
  "description": "Electronic products and gadgets"
}
```

### Step 4: Create product

Endpoint:

```text
POST /api/admin/products
```

Body:

```json
{
  "name": "iPhone 15",
  "description": "Apple smartphone",
  "price": 79999,
  "stock": 10,
  "brand": "Apple",
  "categoryId": 1,
  "imageUrl": "https://example.com/iphone.jpg"
}
```

### Step 5: Register customer

Endpoint:

```text
POST /api/auth/register
```

Body:

```json
{
  "name": "ShopEase Customer",
  "email": "user@example.com",
  "password": "User@1234"
}
```

Copy the customer token and authorize Swagger again with the customer token.

### Step 6: Add product to cart

Endpoint:

```text
POST /api/cart/add
```

Body:

```json
{
  "productId": 1,
  "quantity": 2
}
```

### Step 7: Place order

Endpoint:

```text
POST /api/orders/place
```

Body:

```json
{
  "shippingAddress": "42 Market Road, Mumbai, India",
  "paymentMethod": "COD"
}
```

Allowed payment methods:

```text
COD
CARD
UPI
```

## API summary

### Auth APIs

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Product and category APIs

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| GET | `/api/categories` | Public |

### Cart APIs

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/cart/add` | USER or ADMIN |
| GET | `/api/cart` | USER or ADMIN |
| PUT | `/api/cart/items/{itemId}` | USER or ADMIN |
| DELETE | `/api/cart/items/{itemId}` | USER or ADMIN |
| DELETE | `/api/cart` | USER or ADMIN |

### Order APIs

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/orders/place` | USER or ADMIN |
| GET | `/api/orders/user` | USER or ADMIN |
| GET | `/api/orders/{id}` | USER or ADMIN |

### Admin APIs

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/admin/categories` | ADMIN |
| PUT | `/api/admin/categories/{id}` | ADMIN |
| DELETE | `/api/admin/categories/{id}` | ADMIN |
| POST | `/api/admin/products` | ADMIN |
| PUT | `/api/admin/products/{id}` | ADMIN |
| DELETE | `/api/admin/products/{id}` | ADMIN |
| POST | `/api/admin/products/{id}/image` | ADMIN |
| GET | `/api/admin/orders` | ADMIN |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN |

## Product search examples

```text
GET /api/products?name=iphone
GET /api/products?brand=Apple
GET /api/products?categoryId=1
GET /api/products?page=0&size=5&sort=price,asc
GET /api/products?page=0&size=5&sort=createdAt,desc
```

## Order and payment values

Allowed payment methods:

```text
COD
CARD
UPI
```

Allowed order statuses:

```text
PENDING
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

## Run tests

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

The test suite checks:

- Registration and login
- Admin login
- Public product listing
- Protected admin APIs
- Category creation
- Product creation
- Cart add/update behavior
- Order placement
- User order history
- Admin order status update
- Frontend page loading
- OpenAPI docs endpoint
- Health endpoint

## Run with Docker and MySQL

Start Docker Desktop, then run:

```bash
docker compose up --build
```

The app runs at:

```text
http://127.0.0.1:8080/
```

MySQL values from `docker-compose.yml`:

```text
Database: shopease
Username: shopease
Password: shopease
Root password: root
```

Stop containers:

```bash
docker compose down
```

Stop and remove database volume:

```bash
docker compose down -v
```

## Deploy online on Render

This project includes:

```text
render.yaml
```

That file prepares the project for Render deployment using Docker.

### Deployment steps

1. Push latest code to GitHub.
2. Open:

   ```text
   https://render.com
   ```

3. Login or create an account.
4. Click `New`.
5. Click `Blueprint`.
6. Connect GitHub.
7. Select:

   ```text
   Dinesh12328/shopease-backend-api
   ```

8. Render detects `render.yaml`.
9. Enter a private value for `ADMIN_PASSWORD`.
10. Deploy the service.

After deployment, Render gives a URL like:

```text
https://shopease-backend-api.onrender.com/
```

Online URLs:

```text
Frontend: https://shopease-backend-api.onrender.com/
Swagger:  https://shopease-backend-api.onrender.com/swagger-ui/index.html
Health:   https://shopease-backend-api.onrender.com/actuator/health
```

Important: the basic Render deployment uses H2, so data can reset when the service restarts. For permanent production data, connect a hosted MySQL database.

## Configuration

Main config file:

```text
src/main/resources/application.yml
```

Important values:

```yaml
server:
  port: ${PORT:${SERVER_PORT:8080}}

spring:
  datasource:
    url: ${DB_URL:jdbc:h2:mem:shopease;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}

app:
  admin:
    email: ${ADMIN_EMAIL:admin@shopease.com}
    password: ${ADMIN_PASSWORD:Admin@123}
```

For MySQL, set:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
DB_DRIVER
H2_CONSOLE=false
```

## Troubleshooting

### Frontend does not open

Check that IntelliJ console shows:

```text
Started ShopEaseApplication
```

Then open:

```text
http://127.0.0.1:8080/
```

### Swagger says access denied

Use:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

Swagger and OpenAPI routes are allowed in Spring Security.

### Login fails

For local testing, use:

```text
admin@shopease.com
Admin@123
```

For online Render deployment, use the password you entered in `ADMIN_PASSWORD`.

### 401 Unauthorized

You are calling a protected API without a valid JWT token.

Fix:

1. Login.
2. Copy the token.
3. Use `Authorization: Bearer <token>`.

### 403 Forbidden

Your token is valid, but your role does not have permission.

Examples:

- Admin APIs require `ADMIN`.
- Cart and order APIs require `USER` or `ADMIN`.

### Port 8080 already in use

Change this in `application.yml`:

```yaml
server:
  port: 9090
```

Then open:

```text
http://127.0.0.1:9090/
```

## Project notes

- The local setup uses H2 so the project can run quickly without installing a database.
- The Docker setup can run the application with MySQL.
- The Render setup is included for a simple online deployment.
- The payment flow is simulated for this version. It stores payment records but does not connect to a real payment gateway.
- Product image upload support exists on the backend, while the main UI uses image URLs for simpler testing.
- For a production deployment, replace H2 with a hosted database and set strong private values for all environment variables.
