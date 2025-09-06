# rabbit-request-reply-api


## Testing

```http
POST http://localhost:8080/api/process
Content-Type: text/plain

Hello, Imran!!

###
POST http://localhost:8081/api/process
Content-Type: text/plain

Hello, Test!!

###
GET http://localhost:8080/actuator/health
```