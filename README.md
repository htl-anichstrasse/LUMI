# Backend

## API Endpoints

### Authentication Endpoints
- **POST /auth/register**: Register a new user.
    - **Body**: 
        - `firstname` (string): User's first name.
        - `lastname` (string): User's last name.
        - `username` (string): Desired username.
        - `email` (string): User's email address.
        - `password` (string): Desired password.
        - `img` (string, optional): Base64 encoded image.
    - **Response**: 
        - `201 Created`: `{"message": "User registered successfully.", "access_token": "token", "refresh_token": "token"}`
        - `400 Bad Request`: `{"message": "Error message"}`

- **POST /auth/login**: Login a user.
    - **Body**: 
        - `email` (string): User's email address.
        - `password` (string): User's password.
    - **Response**: 
        - `200 OK`: `{"message": "Login successful", "access_token": "token", "refresh_token": "token"}`
        - `400 Bad Request`: `{"message": "Email and password are required"}`
        - `401 Unauthorized`: `{"message": "Invalid email or password"}`

- **POST /auth/logout**: Logout a user.
    - **Response**: 
        - `200 OK`: `{"message": "Successfully logged out"}`

- **GET /auth/get-user**: Get user details.
    - **Response**: 
        - `200 OK`: `{"firstname": "John", "lastname": "Doe", "img": "Base64String", "username": "johndoe", "email": "john.doe@example.com", "created_at": "2023-01-01T00:00:00Z"}`
        - `404 Not Found`: `{"message": "User not found"}`

- **POST /auth/refresh**: Refresh access token.
    - **Response**: 
        - `200 OK`: `{"access_token": "new_token"}`

---

### Message Endpoints
- **POST /api/message**: Save incoming messages.
    - **Body**: 
        - `type` (string): Type of the message.
        - `content` (string): Content of the message.
        - `title` (string): Title of the message.
        - `postTime` (timestamp): Time the message was posted.
        - `package` (string): Package information.
        - `encrypted_content` (string): Encrypted content.
        - `encrypted_title` (string): Encrypted title.
    - **Response**: 
        - `201 Created`: `{"message": "Message received", "keywords": ["keyword1", "keyword2"]}`
        - `400 Bad Request`: `{"error": "All fields are required"}`

---

### Vector Endpoints
- **GET /api/vectors**: Get all vectors.
    - **Response**: 
        - `200 OK`: `{"vector_data": [{"text": "example", "time": "2023-01-01T00:00:00Z"}]}`
        - `404 Not Found`: `{"message": "No vectors found"}`

- **DELETE /api/vectors**: Delete all vectors.
    - **Response**: 
        - `200 OK`: `{"message": "All vectors deleted"}`

- **GET /api/vector/<int:id>**: Get a vector by ID.
    - **Param**: 
        - `id` (int): Vector ID.
    - **Response**: 
        - `200 OK`: `{"text": "example", "time": "2023-01-01T00:00:00Z"}`
        - `404 Not Found`: `{"message": "Vector not found"}`

- **DELETE /api/vector/<int:id>**: Delete a vector by ID.
    - **Param**: 
        - `id` (int): Vector ID.
    - **Response**: 
        - `200 OK`: `{"message": "Vector deleted"}`
        - `404 Not Found`: `{"message": "Vector not found"}`

---

### Geo Data Endpoints
- **GET /api/geo**: Get geo data within a date range.
    - **Query Params**: 
        - `from_date` (string): Start date.
        - `to_date` (string): End date.
    - **Response**: 
        - `200 OK`: `{"geo_data": [{"data": "example", "created_at": "2023-01-01T00:00:00Z"}]}`
        - `400 Bad Request`: `{"message": "From date and to date required"}`
        - `404 Not Found`: `{"message": "No geo data found"}`

- **GET /api/geo/<int:id>**: Get geo data by ID.
    - **Param**: 
        - `id` (int): Geo data ID.
    - **Response**: 
        - `200 OK`: `{"data": "example", "created_at": "2023-01-01T00:00:00Z"}`
        - `404 Not Found`: `{"message": "Geo data not found"}`

- **POST /api/geo**: Save geo data.
    - **Body**: 
        - `data` (string): Geo data.
    - **Response**: 
        - `201 Created`: `{"message": "Geo data saved"}`
        - `400 Bad Request`: `{"message": "Data required"}`

- **DELETE /api/geo/<int:id>**: Delete geo data by ID.
    - **Param**: 
        - `id` (int): Geo data ID.
    - **Response**: 
        - `200 OK`: `{"message": "Geo data deleted"}`
        - `404 Not Found`: `{"message": "Geo data not found"}`

---

### Query Endpoints
- **GET /api/query**: Query the system.
    - **Query Params**: 
        - `message` (string): Query message.
        - `longitude` (float): Longitude.
        - `latitude` (float): Latitude.
    - **Response**: 
        - `200 OK`: `{"result": "Query result"}`
        - `400 Bad Request`: `{"error": "Missing message content"}`

---

### Mensa Endpoints
- **GET /api/mensa**: Get the weekly menu from the HTL Mensa.
    - **Response**: 
        - `200 OK`: `{"menu": {"Monday": {"soup": "Tomato Soup", "dish1": "Pasta", "dish2": "Pizza", "dish3": "Salad"}}}`
        - `500 Internal Server Error`: `{"error": "Failed to fetch menu"}`

---

### Generate Response Endpoint
- **GET /api/generate_response**: Generate an AI response.
    - **Query Params**: 
        - `question` (string): The question to ask.
        - `context` (string): The context for the question.
    - **Response**: 
        - `200 OK`: `{"answer": "This is the AI-generated answer"}`
        - `400 Bad Request`: `{"error": "Missing question or context parameter"}`
        - `500 Internal Server Error`: `{"error": "Failed to generate answer"}`
