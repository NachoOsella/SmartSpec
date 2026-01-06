# Development Dockerfile for Angular
FROM node:20-alpine

WORKDIR /app

# Copy package files first (better cache utilization)
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Expose port 4200
EXPOSE 4200

# Start the development server
CMD ["npm", "start", "--", "--host", "0.0.0.0", "--disable-host-check"]
