# ============================================================================
# Dockerfile for ReleaseSense Python Machine Learning Prediction Service
# ============================================================================

FROM python:3.11-slim

WORKDIR /app

# Install system utilities for health checks
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source code, dataset, and assets
COPY . .

# Train model if model file is not already present
RUN python train_model.py

# Expose microservice port
EXPOSE 5001

ENV PORT=5001
ENV PYTHONUNBUFFERED=1

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:5001/health || exit 1

# Start Flask prediction service
CMD ["python", "app.py"]
