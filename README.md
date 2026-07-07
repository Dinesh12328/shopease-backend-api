# ShopEase Backend API

ShopEase is a complete e-commerce backend project built with Java, Spring Boot, Spring Security, JWT, Spring Data JPA, Hibernate, Swagger/OpenAPI, H2/MySQL, and Docker.

It provides APIs for user registration/login, product browsing, product search, category management, cart management, order placement, payment records, and admin product/order management.

## 1. Project overview

This project is designed like a real online shopping backend.

- Customers can register, login, browse products, add products to cart, and place orders.
- Admins can login, create categories, add products, update products, delete products, upload product images, and manage order status.
- JWT authentication protects private APIs.
- Role-based authorization separates `USER` and `ADMIN` access.
- Swagger UI is included for easy browser-based API testing.
- The default local database is H2, so you can run the project immediately without installing MySQL.
- MySQL and Docker Compose are included for a more production-style setup.

## 2. Main technologies

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security
- JWT authentication
- Spring Data JPA
- Hibernate
- H2 database for local testing
- MySQL for Docker or production-style running
- Swagger/OpenAPI
- Maven
- Docker and Docker Compose

## 3. Main modules

```text
Auth module       -> register, login, JWT token generation
User module       -> user records and role handling
Category module   -> product categories
Product module    -> product CRUD, search, pagination, sorting, image URL
Cart module       -> user cart and cart items
Order module      -> place orders and view order history
Payment module    -> COD/CARD/UPI payment record simulation
Admin module      -> admin-only product, category, and order management
Security module   -> JWT filter, role authorization, password encryption
```

## 4. How the project works internally

The request flow is:

```text
Browser / Swagger / Postman
        |
        v
Controller
        |
        v
Service
        |
        v
Repository
        |
        v
Database
```

Example: placing an order

```text
User logs in
  -> receives JWT token
  -> adds product to cart
  -> places order
  -> app checks stock
  -> app creates order and order items
  -> app creates payment record
  -> app reduces product stock
  -> user can see order history
```

## 5. Important database entities

- `User`
- `Product`
- `Category`
- `Cart`
- `CartItem`
- `Order`
- `OrderItem`
- `Payment`

Relationships:

- One user has one cart.
- One cart has many cart items.
- One product belongs to one category.
- One order belongs to one user.
- One order has many order items.
- One order has one payment record.

## 6. Project folder structure

```text
src/main/java/com/shopease
  config       -> Spring Security, Swagger, web config, default admin setup
  controller   -> REST API controllers
  dto          -> request/response objects
  entity       -> JPA database entities
  exception    -> global error handling
  repository   -> Spring Data JPA repositories
  security     -> JWT service and JWT filter
  service      -> business logic

src/main/resources
  application.yml

src/test
  integration tests

Dockerfile
docker-compose.yml
pom.xml
README.md
```

## 7. Default local credentials

When the application starts, it automatically creates one admin account if it does not already exist.

```text
Admin email:    admin@shopease.com
Admin password: Admin@123
```

For customer testing, create a new user through the register API.

## 8. Required software

Install these before running the project:

1. Java 17 or newer
2. IntelliJ IDEA
3. Maven

Optional:

4. Docker Desktop, only if you want to run with MySQL using Docker
5. Postman, only if you prefer Postman instead of Swagger

## 9. Run the project in IntelliJ IDEA

Use these exact steps:

1. Open IntelliJ IDEA.
2. Click `File` -> `Open`.
3. Select the project folder:

   ```text
   shopease-backend-api-project-type
   ```

4. Wait for IntelliJ to load Maven dependencies.
5. Make sure the selected JDK is Java 17 or newer:

   ```text
   File -> Project Structure -> Project SDK -> Java 17+
   ```

6. Open this file:

   ```text
   src/main/java/com/shopease/ShopEaseApplication.java
   ```

7. Click the green run button near `public static void main`.
8. Wait until the IntelliJ console shows something like:

   ```text
   Tomcat started on port 8080
   Started ShopEaseApplication
   ```

9. Keep IntelliJ running. Do not stop the application while testing APIs.

## 10. Run the project from terminal

From the project root folder, run:

```bash
mvn spring-boot:run
```

Or build a jar:

```bash
mvn clean package
```

Then run:

```bash
java -jar target/shopease-api-1.0.0.jar
```

## 11. Important local URLs

Open these in your normal system browser such as Chrome, Edge, or Firefox.

If the Codex in-app browser blocks localhost, use your real browser instead.

| Purpose | URL |
|---|---|
| Swagger UI | `http://127.0.0.1:8080/swagger-ui/index.html` |
| Swagger UI alternate | `http://localhost:8080/swagger-ui.html` |
| Health check | `http://127.0.0.1:8080/actuator/health` |
| OpenAPI JSON | `http://127.0.0.1:8080/v3/api-docs` |
| H2 database console | `http://127.0.0.1:8080/h2-console` |

Health check success response:

```json
{
  "status": "UP"
}
```

## 12. H2 database login

The default local setup uses H2 in-memory database. You do not need MySQL for normal testing.

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

Leave the password empty.

Important: because H2 is in-memory, data resets when the application stops.

## 13. Swagger testing step by step

Open Swagger:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

### Step 1: Login as admin

Open:

```text
POST /api/auth/login
```

Request body:

```json
{
  "email": "admin@shopease.com",
  "password": "Admin@123"
}
```

Click `Execute`.

Copy the token from:

```text
data.token
```

### Step 2: Add JWT token in Swagger

At the top/right of Swagger, click `Authorize`.

Paste only the token value.

If Swagger asks for bearer auth, either of these usually works:

```text
eyJhbGciOiJIUzI1NiJ9...
```

or:

```text
Bearer eyJhbGciOiJIUzI1NiJ9...
```

Then click `Authorize` and close the popup.

### Step 3: Create a category as admin

Open:

```text
POST /api/admin/categories
```

Request body:

```json
{
  "name": "Electronics",
  "description": "Electronic items and gadgets"
}
```

Click `Execute`.

Copy the returned category ID, for example:

```text
data.id = 1
```

### Step 4: Create a product as admin

Open:

```text
POST /api/admin/products
```

Request body:

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

Click `Execute`.

Copy the product ID, for example:

```text
data.id = 1
```

### Step 5: View products publicly

Open:

```text
GET /api/products
```

Click `Execute`.

This API is public. Login is not required.

### Step 6: Search products

Try these URLs in browser or Swagger:

```text
GET /api/products?name=iphone
GET /api/products?brand=Apple
GET /api/products?categoryId=1
GET /api/products?page=0&size=5&sort=price,asc
GET /api/products?page=0&size=5&sort=createdAt,desc
```

### Step 7: Register a customer

Open:

```text
POST /api/auth/register
```

Request body:

```json
{
  "name": "Demo User",
  "email": "user@example.com",
  "password": "User@1234"
}
```

Click `Execute`.

Copy the customer token from:

```text
data.token
```

Use `Authorize` again in Swagger and replace the admin token with the customer token.

### Step 8: Add product to cart

Open:

```text
POST /api/cart/add
```

Request body:

```json
{
  "productId": 1,
  "quantity": 2
}
```

Click `Execute`.

### Step 9: View cart

Open:

```text
GET /api/cart
```

Click `Execute`.

### Step 10: Place order

Open:

```text
POST /api/orders/place
```

Request body:

```json
{
  "shippingAddress": "123 Demo Street, Mumbai, India",
  "paymentMethod": "COD"
}
```

Allowed payment methods:

```text
COD
CARD
UPI
```

Click `Execute`.

### Step 11: View customer order history

Open:

```text
GET /api/orders/user
```

Click `Execute`.

### Step 12: Admin updates order status

Authorize again with the admin token.

Open:

```text
PATCH /api/admin/orders/{id}/status
```

Request body:

```json
{
  "status": "CONFIRMED"
}
```

Allowed order statuses:

```text
PENDING
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

## 14. API list

### Auth APIs

| Method | URL | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Public product/category APIs

| Method | URL | Access |
|---|---|---|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| GET | `/api/categories` | Public |

### User cart APIs

| Method | URL | Access |
|---|---|---|
| POST | `/api/cart/add` | USER or ADMIN |
| GET | `/api/cart` | USER or ADMIN |
| PUT | `/api/cart/items/{itemId}` | USER or ADMIN |
| DELETE | `/api/cart/items/{itemId}` | USER or ADMIN |
| DELETE | `/api/cart` | USER or ADMIN |

### User order APIs

| Method | URL | Access |
|---|---|---|
| POST | `/api/orders/place` | USER or ADMIN |
| GET | `/api/orders/user` | USER or ADMIN |
| GET | `/api/orders/{id}` | USER or ADMIN |

### Admin APIs

| Method | URL | Access |
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

## 15. Request body examples

### Register

```json
{
  "name": "Demo User",
  "email": "user@example.com",
  "password": "User@1234"
}
```

### Login

```json
{
  "email": "admin@shopease.com",
  "password": "Admin@123"
}
```

### Create category

```json
{
  "name": "Fashion",
  "description": "Clothes and accessories"
}
```

### Create product

```json
{
  "name": "Running Shoes",
  "description": "Comfortable sports shoes",
  "price": 2499,
  "stock": 25,
  "brand": "Nike",
  "categoryId": 1,
  "imageUrl": "https://example.com/shoes.jpg"
}
```

### Add item to cart

```json
{
  "productId": 1,
  "quantity": 1
}
```

### Update cart item

```json
{
  "quantity": 3
}
```

### Place order

```json
{
  "shippingAddress": "221B Baker Street, London",
  "paymentMethod": "COD"
}
```

### Update order status

```json
{
  "status": "SHIPPED"
}
```

## 16. Run tests

From the project root:

```bash
mvn test
```

Expected result:

```text
BUILD SUCCESS
```

## 17. Run with Docker and MySQL

Use this if you want MySQL instead of H2.

Start Docker Desktop first, then run:

```bash
docker compose up --build
```

The API will run at:

```text
http://127.0.0.1:8080
```

MySQL runs on:

```text
localhost:3306
```

Docker database values from `docker-compose.yml`:

```text
Database: shopease
Username: shopease
Password: shopease
Root password: root
```

Stop Docker:

```bash
docker compose down
```

Stop Docker and delete MySQL data:

```bash
docker compose down -v
```

## 18. Local configuration

Main config file:

```text
src/main/resources/application.yml
```

Important settings:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:shopease

app:
  admin:
    email: admin@shopease.com
    password: Admin@123
```

You can change the port if 8080 is busy:

```yaml
server:
  port: 9090
```

Then Swagger becomes:

```text
http://127.0.0.1:9090/swagger-ui/index.html
```

## 19. Troubleshooting

### Problem: Swagger says Access Denied

Make sure you are using the latest source code. Swagger, OpenAPI docs, health check, H2 console, public products, and public categories are allowed in Spring Security.

Then restart the application from IntelliJ.

Use:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

### Problem: This site cannot be reached

The application is probably not running.

Check IntelliJ console. You must see:

```text
Started ShopEaseApplication
```

Then open:

```text
http://127.0.0.1:8080/actuator/health
```

### Problem: Codex in-app browser blocks localhost

Use Chrome, Edge, or Firefox outside Codex.

Open:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

### Problem: Port 8080 is already used

Either stop the other application using port 8080, or change this in `application.yml`:

```yaml
server:
  port: 9090
```

### Problem: Login fails

Use the default admin:

```text
admin@shopease.com
Admin@123
```

If you changed credentials in environment variables or Docker, use those new values.

### Problem: 401 Unauthorized

You are calling a protected API without JWT token.

Login first, copy `data.token`, click `Authorize` in Swagger, and paste the token.

### Problem: 403 Forbidden

Your token is valid, but your role does not have permission.

Examples:

- `POST /api/admin/products` requires `ADMIN`.
- `POST /api/cart/add` works for `USER` or `ADMIN`.

## 20. Resume explanation

You can describe this project like this:

```text
Built a complete e-commerce backend using Java Spring Boot with JWT authentication,
role-based authorization, product/category management, cart, order placement,
payment records, Swagger documentation, JPA/Hibernate relationships, pagination,
sorting, validation, exception handling, H2/MySQL database support, and Docker.
```

This project demonstrates:

- REST API development
- CRUD operations
- Authentication and authorization
- JWT security
- Database relationships
- Layered backend architecture
- Spring Data JPA repositories
- Validation and exception handling
- API documentation with Swagger
- Docker-based deployment setup

