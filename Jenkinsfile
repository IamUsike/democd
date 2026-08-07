pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/room107_Agileish.git'
        BRANCH = 'dev'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }

        stage('Prepare Env') {
            steps {
                // .env is gitignored — compose needs MYSQL_ROOT_PASSWORD / DB_* /
                // VITE_API_BASE_URL. Seed from example if the agent has none yet.
                sh '''
                    if [ ! -f .env ]; then
                      cp .env.example .env
                      echo "Created .env from .env.example"
                    else
                      echo "Using existing .env on agent"
                    fi
                    # Fail fast if MySQL password is still empty after expansion
                    set -a
                    . ./.env
                    set +a
                    if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
                      echo "MYSQL_ROOT_PASSWORD is empty — set it in .env on the agent"
                      exit 1
                    fi
                '''
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
                sh 'curl -f http://localhost:8082/ || exit 1'
            }
        }
    }
}