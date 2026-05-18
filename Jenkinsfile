pipeline {
    agent any

    environment {
        PROJECT_NAME = 'TraceNet'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Verify Project Structure') {
            steps {
                echo 'Verifying TraceNet service folders...'
                sh '''
                    test -d services/api-gateway
                    test -d services/auth-service
                    test -d services/trace-ingestion-service
                    test -d services/trace-query-service
                    test -d services/analytics-service
                    test -d services/alert-service
                    test -d services/demo-order-service
                    test -d services/demo-payment-service
                    test -d services/demo-inventory-service
                    test -f docker-compose.yml
                '''
            }
        }

        stage('Build Maven Services') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        sh 'cd services/api-gateway && mvn clean package -DskipTests'
                    }
                }

                stage('Build Auth Service') {
                    steps {
                        sh 'cd services/auth-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Trace Ingestion Service') {
                    steps {
                        sh 'cd services/trace-ingestion-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Trace Query Service') {
                    steps {
                        sh 'cd services/trace-query-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Analytics Service') {
                    steps {
                        sh 'cd services/analytics-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Alert Service') {
                    steps {
                        sh 'cd services/alert-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Demo Order Service') {
                    steps {
                        sh 'cd services/demo-order-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Demo Payment Service') {
                    steps {
                        sh 'cd services/demo-payment-service && mvn clean package -DskipTests'
                    }
                }

                stage('Build Demo Inventory Service') {
                    steps {
                        sh 'cd services/demo-inventory-service && mvn clean package -DskipTests'
                    }
                }
            }
        }

        stage('Docker Compose Build') {
            steps {
                echo 'Building Docker images using docker compose...'
                sh 'docker compose build'
            }
        }

        stage('Start Stack') {
            steps {
                echo 'Starting TraceNet stack...'
                sh 'docker compose up -d'
            }
        }

        stage('Wait For Services') {
            steps {
                echo 'Waiting for services to initialize...'
                sh 'sleep 45'
            }
        }

        stage('Gateway Health Check') {
            steps {
                echo 'Checking API Gateway health...'
                sh '''
                    curl --fail --silent http://localhost:8080/actuator/health
                '''
            }
        }

        stage('Smoke Test Auth Route') {
            steps {
                echo 'Testing auth route through API Gateway...'
                sh '''
                    curl --fail --silent -X POST http://localhost:8080/api/auth/register \
                    -H "Content-Type: application/json" \
                    -d '{
                      "email": "jenkins-sre@tracenet.com",
                      "password": "password123",
                      "orgId": "org-demo",
                      "role": "SRE"
                    }' || true

                    TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
                    -H "Content-Type: application/json" \
                    -d '{
                      "email": "jenkins-sre@tracenet.com",
                      "password": "password123"
                    }' | sed -n 's/.*"token":"\\([^"]*\\)".*/\\1/p')

                    test -n "$TOKEN"
                    echo "JWT token generated successfully"
                '''
            }
        }

        stage('Smoke Test Trace Flow') {
            steps {
                echo 'Testing demo trace generation and analytics...'
                sh '''
                    TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
                    -H "Content-Type: application/json" \
                    -d '{
                      "email": "jenkins-sre@tracenet.com",
                      "password": "password123"
                    }' | sed -n 's/.*"token":"\\([^"]*\\)".*/\\1/p')

                    curl --fail --silent -X POST http://localhost:8082/orders
                    curl --fail --silent http://localhost:8080/api/analytics/summary \
                    -H "Authorization: Bearer $TOKEN"
                '''
            }
        }
    }

    post {
        always {
            echo 'Collecting Docker container status...'
            sh 'docker compose ps || true'
        }

        success {
            echo 'TraceNet CI/CD pipeline completed successfully.'
        }

        failure {
            echo 'TraceNet CI/CD pipeline failed. Showing recent logs...'
            sh 'docker compose logs --tail=100 || true'
        }
    }
}