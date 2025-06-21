# Use the official Node.js 18 LTS image
FROM node:18-alpine

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json (if available)
COPY package*.json ./

# Install dependencies
RUN npm ci --omit=dev

# Copy the rest of the application code
COPY . .

# Create uploads directory
RUN mkdir -p uploads logs

# Create a non-root user to run the application
RUN addgroup -g 1001 -S nodejs
RUN adduser -S heroesapi -u 1001

# Change ownership of the app directory to the nodejs user
RUN chown -R heroesapi:nodejs /app

# Switch to the non-root user
USER heroesapi

# Expose the port the app runs on
EXPOSE 3000

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node healthcheck.js

# Start the application
CMD ["npm", "start"]