# Home Security Service (Backend)

## Overview

The **Home Security Service** is a Spring Boot application deployed on Digital Ocean that provides cloud storage for captured images from the ESP32-CAM. It offers a REST API for uploading, retrieving, and managing images with support for both local filesystem and AWS S3 storage.

**Framework**: Spring Boot 2.6+  
**Database**: PostgreSQL  
**Storage**: Local filesystem or AWS S3  
**Deployment**: Digital Ocean Droplet (Ubuntu 20.04)

---

## Key Features

- 📤 **Image Upload**: Multipart file upload with metadata
- 📥 **Image Retrieval**: Download images by ID
- 📋 **Image Listing**: Paginated list with sorting and filtering
- 🗑️ **Image Deletion**: Remove images from storage and database
- 💾 **Dual Storage**: Local filesystem or AWS S3
- 🔍 **Search**: Find images by date, size, etc.
- 🗄️ **Database**: PostgreSQL for metadata
- 🔒 **API Security**: Bearer token authentication (optional)

---

## Architecture

```
[ESP32-CAM] ──HTTP POST──► [Old Phone App] ──HTTP POST──► [Digital Ocean Service]
                                                                    │
                                                         ┌──────────┴──────────┐
                                                         │                     │
                                                  [PostgreSQL]          [S3 or Local]
                                                  (Metadata)              (Images)
```

---

## API Endpoints

### POST /upload
Upload an image file.

**Request**:
```http
POST /upload HTTP/1.1
Content-Type: multipart/form-data
Authorization: Bearer YOUR_TOKEN (optional)

file=<binary image data>
```

**Response**:
```json
{
  "id": "abc123",
  "fileId": "20240212103005_capture",
  "fileName": "capture.jpg",
  "contentType": "image/jpeg",
  "fileSize": 87654,
  "storageLocation": "s3://bucket/path/file.jpg",
  "uploadedAt": "2024-02-12T10:30:05Z",
  "url": "https://server.com/files/abc123"
}
```

### GET /files/{id}
Download an image by ID.

**Request**:
```http
GET /files/abc123 HTTP/1.1
```

**Response**: JPEG image (binary)

### GET /files
List all uploaded files (paginated).

**Request**:
```http
GET /files?page=0&size=20&sort=uploadedAt,desc HTTP/1.1
```

**Response**:
```json
{
  "content": [
    {
      "id": "abc123",
      "fileId": "20240212103005_capture",
      "fileName": "capture.jpg",
      "contentType": "image/jpeg",
      "fileSize": 87654,
      "uploadedAt": "2024-02-12T10:30:05Z",
      "url": "https://server.com/files/abc123"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

### DELETE /files/{id}
Delete an image.

**Request**:
```http
DELETE /files/abc123 HTTP/1.1
Authorization: Bearer YOUR_TOKEN (optional)
```

**Response**: 204 No Content

---

## Setup Instructions

### 1. Create Digital Ocean Droplet

1. Log in to Digital Ocean
2. Create → Droplets
3. Choose:
   - **OS**: Ubuntu 20.04 LTS
   - **Plan**: Basic $5/month (1GB RAM)
   - **Datacenter**: Closest to you
   - **Authentication**: SSH key
4. Create Droplet
5. Note IP address

### 2. Connect to Server

```bash
ssh root@your_droplet_ip
```

### 3. Install Dependencies

```bash
# Update system
apt update && apt upgrade -y

# Install Java
apt install openjdk-11-jdk -y
java -version

# Install PostgreSQL
apt install postgresql postgresql-contrib -y
systemctl start postgresql
systemctl enable postgresql
```

### 4. Setup Database

```bash
sudo -u postgres psql

# In PostgreSQL prompt:
CREATE DATABASE homesecurity;
CREATE USER securityuser WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE homesecurity TO securityuser;
\\q
```

### 5. Build Application

On local machine:
```bash
cd home-security-service
./mvnw clean package
```

### 6. Deploy Application

```bash
# Transfer JAR to server
scp target/home-security-service-0.0.1-SNAPSHOT.jar root@your_droplet_ip:/opt/

# Create configuration
ssh root@your_droplet_ip
nano /opt/application.properties
```

Add:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/homesecurity
spring.datasource.username=securityuser
spring.datasource.password=your_secure_password

# Storage (Local)
storage.type=local
storage.local.path=/var/lib/home-security/images

# OR Storage (S3)
# storage.type=s3
# storage.s3.bucket=your-bucket-name
# storage.s3.region=us-east-1
# storage.s3.accessKey=YOUR_ACCESS_KEY
# storage.s3.secretKey=YOUR_SECRET_KEY

# Server
server.port=8080
```

### 7. Create Storage Directory

```bash
mkdir -p /var/lib/home-security/images
chmod 755 /var/lib/home-security/images
```

### 8. Create Systemd Service

```bash
nano /etc/systemd/system/home-security.service
```

Content:
```ini
[Unit]
Description=Home Security Service
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt
ExecStart=/usr/bin/java -jar /opt/home-security-service-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/application.properties
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 9. Start Service

```bash
systemctl daemon-reload
systemctl start home-security
systemctl enable home-security
systemctl status home-security
```

### 10. Configure Firewall

```bash
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 8080/tcp  # Application
ufw enable
ufw status
```

### 11. Test Service

```bash
# From server
curl http://localhost:8080/files

# From external
curl http://your_droplet_ip:8080/files
```

---

## Configuration

### Local Storage
```properties
storage.type=local
storage.local.path=/var/lib/home-security/images
```

Images stored at: `/var/lib/home-security/images/{fileId}.jpg`

### AWS S3 Storage
```properties
storage.type=s3
storage.s3.bucket=your-bucket-name
storage.s3.region=us-east-1
storage.s3.accessKey=YOUR_ACCESS_KEY
storage.s3.secretKey=YOUR_SECRET_KEY
```

### Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/homesecurity
spring.datasource.username=securityuser
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Server Configuration
```properties
server.port=8080
server.max-http-header-size=10MB
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## Database Schema

### FileMetadata Table
```sql
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_id VARCHAR(255) UNIQUE NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    storage_location VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    INDEX idx_file_id (file_id),
    INDEX idx_uploaded_at (uploaded_at)
);
```

---

## Usage Examples

### Upload Image
```bash
curl -X POST \\
  http://your_server:8080/upload \\
  -F "file=@capture.jpg"
```

### Get Image
```bash
curl http://your_server:8080/files/abc123 -o downloaded.jpg
```

### List Images
```bash
curl http://your_server:8080/files?page=0&size=10
```

### Delete Image
```bash
curl -X DELETE http://your_server:8080/files/abc123
```

---

## Monitoring

### View Logs
```bash
# Live logs
journalctl -u home-security -f

# Last 100 lines
journalctl -u home-security -n 100

# Specific time range
journalctl -u home-security --since "2024-02-12 10:00" --until "2024-02-12 11:00"

# Save to file
journalctl -u home-security > service_logs.txt
```

### Check Service Status
```bash
systemctl status home-security
```

### Monitor Disk Usage
```bash
df -h /var/lib/home-security/images
du -sh /var/lib/home-security/images
```

### Monitor Database
```bash
sudo -u postgres psql -d homesecurity -c "SELECT COUNT(*) FROM file_metadata;"
```

---

## Troubleshooting

### Service Won't Start
```bash
# Check logs
journalctl -u home-security -n 50

# Common issues:
# - Port 8080 already in use
# - Database connection failed
# - Storage directory doesn't exist
# - Java not found
```

### Can't Upload Images
```bash
# Check permissions
ls -la /var/lib/home-security/images

# Fix permissions
chmod 755 /var/lib/home-security/images

# Check disk space
df -h

# Check file size limit
# Edit application.properties:
# spring.servlet.multipart.max-file-size=10MB
```

### Database Connection Errors
```bash
# Check PostgreSQL running
systemctl status postgresql

# Test connection
sudo -u postgres psql -d homesecurity -c "SELECT 1;"

# Check credentials in application.properties
```

### High Memory Usage
```bash
# Check memory
free -h

# Adjust JVM heap
# Edit /etc/systemd/system/home-security.service
# ExecStart=/usr/bin/java -Xmx512m -jar ...
```

---

## Maintenance

### Backup Database
```bash
pg_dump homesecurity > backup_$(date +%Y%m%d).sql
```

### Restore Database
```bash
sudo -u postgres psql -d homesecurity < backup_20240212.sql
```

### Clean Old Images
```bash
# Delete images older than 30 days
find /var/lib/home-security/images -type f -mtime +30 -delete

# Update database (manual cleanup)
sudo -u postgres psql -d homesecurity -c "DELETE FROM file_metadata WHERE uploaded_at < NOW() - INTERVAL '30 days';"
```

### Update Application
```bash
# Stop service
systemctl stop home-security

# Backup current JAR
cp /opt/home-security-service-0.0.1-SNAPSHOT.jar /opt/backup/

# Deploy new JAR
scp target/home-security-service-0.0.1-SNAPSHOT.jar root@your_droplet_ip:/opt/

# Start service
systemctl start home-security

# Check status
systemctl status home-security
```

---

## Security

### Current Implementation
- No authentication
- HTTP only (not HTTPS)
- No rate limiting

### Recommended Improvements
```properties
# Add authentication
security.api.key=your_random_key_here

# Enable HTTPS (requires certificate)
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
```

### Nginx Reverse Proxy (HTTPS)
```nginx
server {
    listen 443 ssl;
    server_name your_domain.com;

    ssl_certificate /etc/letsencrypt/live/your_domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your_domain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## Development

### Requirements
- Java JDK 11
- Maven 3.6+
- PostgreSQL 12+
- IDE (IntelliJ IDEA recommended)

### Build
```bash
./mvnw clean package
```

### Run Locally
```bash
./mvnw spring-boot:run
```

### Run Tests
```bash
./mvnw test
```

---

## Related Documentation

- [ESP32-CAM](../esp32-camera/README.md) - Image source
- [Home Client App](../home-client-android/README.md) - Upload coordinator
- [API Reference](../API-REFERENCE.md) - Complete API docs
- [Setup Guide](../SETUP-GUIDE.md) - Deployment guide
- [Security Guide](../SECURITY.md) - Security improvements

---

**Framework**: Spring Boot 2.6+  
**Database**: PostgreSQL  
**Port**: 8080  
**Version**: 1.0  
**Status**: Production Ready
