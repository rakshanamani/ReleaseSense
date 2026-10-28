pipeline {
    agent any

    environment {
        // Build & Versioning Configuration
        APP_NAME                 = 'releasesense'
        REGISTRY_HOST            = 'docker.io'
        REGISTRY_USER            = 'rakshana16'
        IMAGE_WEB                = "${REGISTRY_USER}/releasesense-web"
        IMAGE_ML                 = "${REGISTRY_USER}/releasesense-ml"
        BUILD_TAG                = "${env.BUILD_NUMBER}"

        // Jenkins Credentials IDs (configured in Jenkins Credentials Store)
        GIT_CREDENTIALS_ID       = 'github-access-token'
        REGISTRY_CREDENTIALS_ID  = 'dockerhub-credentials'
        KUBE_CREDENTIALS_ID      = 'k8s-kubeconfig'
        MINIKUBE_HOME = 'C:\\Users\\raksh'
KUBECONFIG = 'C:\\Users\\raksh\\.kube\\config'
    KUBECTL_HOME = 'C:\\Users\\raksh\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()
    }

    stages {
        // ====================================================================
        // Stage 1: Source Control Checkout
        // ====================================================================
        stage('Checkout SCM') {
            steps {
                echo "==> [Stage 1/8] Checking out repository from GitHub..."
                checkout scm
            }
        }

        // ====================================================================
        // Stage 2: Python Environment & Dependencies
        // ====================================================================
      stage('Python Setup & ML Dependencies') {
    steps {
        echo "==> [Stage 2/8] Setting up Python virtual environment and installing dependencies..."
        dir('ml-service') {
            bat '''
                "C:\\Program Files\\Python314\\python.exe" -m venv venv
                venv\\Scripts\\python.exe -m pip install --upgrade pip
                venv\\Scripts\\python.exe -m pip install -r requirements.txt
            '''
        }
    }
}

        // ====================================================================
        // Stage 3: ML Model Training & Unit Evaluation
        // ====================================================================
        stage('Train & Evaluate ML Model') {
    steps {
        echo "==> [Stage 3/8] Training Random Forest classifier and validating test benchmarks..."
        dir('ml-service') {
            bat '''
                venv\\Scripts\\python.exe train_model.py
                venv\\Scripts\\python.exe test_api.py
            '''
        }
    }
}

        // ====================================================================
        // Stage 4: Java Maven Build & Unit Testing
        // ====================================================================
        stage('Java Maven Build & Test') {
            steps {
                echo "==> [Stage 4/8] Compiling Java 17 Jakarta Servlets and running JUnit test suite..."
                bat 'mvn clean test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        // ====================================================================
        // Stage 5: Package Java Web Archive (WAR)
        // ====================================================================
        stage('Package WAR') {
            steps {
                echo "==> [Stage 5/8] Packaging Tomcat 11 WAR distribution..."
                bat 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        // ====================================================================
        // Stage 6: Docker Image Creation
        // ====================================================================
      stage('Docker Image Creation') {
    steps {
        echo "==> [Stage 6/8] Building Docker container images..."
        bat """
            "C:\\Users\\raksh\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" build -t ${IMAGE_ML}:${BUILD_TAG} -t ${IMAGE_ML}:latest ./ml-service

            "C:\\Users\\raksh\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" build -t ${IMAGE_WEB}:${BUILD_TAG} -t ${IMAGE_WEB}:latest .
        """
    }
}

        // ====================================================================
        // Stage 7: Docker Image Publishing to Registry
        // ====================================================================
       
stage('Publish Docker Images') {
    steps {
        echo "==> [Stage 7/8] Loading Docker images into Minikube..."

        bat '''
            "C:\\Users\\raksh\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" images

            "C:\\Program Files\\Kubernetes\\Minikube\\minikube.exe" image load %IMAGE_ML%:latest

            "C:\\Program Files\\Kubernetes\\Minikube\\minikube.exe" image load %IMAGE_WEB%:latest
        '''
    }
}


        // ====================================================================
        // Stage 8: Kubernetes Deployment (Minikube / Cluster)
        // ====================================================================
      
stage('Kubernetes Deployment') {
    steps {
        echo "==> [Stage 8/8] Deploying ReleaseSense workloads to Minikube..."

        bat '''
            "%KUBECTL_HOME%\kubectl.exe" apply -f k8s/ml-deployment.yaml
"%KUBECTL_HOME%\kubectl.exe" apply -f k8s/web-deployment.yaml
"%KUBECTL_HOME%\kubectl.exe" apply -f k8s/services.yaml
"%KUBECTL_HOME%\kubectl.exe" rollout status deployment/releasesense-ml --timeout=180s
"%KUBECTL_HOME%\kubectl.exe" rollout status deployment/releasesense-web --timeout=180s
        '''
    }
}

    }

    post {
        success {
            echo "SUCCESS: ReleaseSense CI/CD Pipeline completed successfully for build #${env.BUILD_NUMBER}."
        }
        failure {
            echo "FAILURE: ReleaseSense CI/CD Pipeline failed on build #${env.BUILD_NUMBER}. Check logs for details."
        }
        cleanup {
            cleanWs()
        }
    }
}
