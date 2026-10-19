pipeline {
    agent any

    post {
        success {
            echo 'Pipeline finished: SUCCESS'
        }
        failure {
            echo 'Pipeline finished: FAILURE'
        }
    }

    stages {
        // Checkout uzhe vypolnyaet SCM-plagin Jenkins (Pipeline script from SCM)
        // pri pomoschi 'Pipeline script from SCM' i ukazannogo Repository URL.
        // Manualnyy 'bat copy' ne nuzhen — eto byl obkhod dlya LR3 bez Git.

        stage('Build') {
            steps {
                echo 'Stage: Build -> mvn clean compile'
                bat '"C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B -DskipTests clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Stage: Test -> mvn test'
                bat '"C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B test'
            }
        }

        stage('Package') {
            steps {
                echo 'Stage: Package -> mvn package -DskipTests'
                bat '"C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B package -DskipTests'
            }
        }

        stage('Verify JAR') {
            steps {
                echo 'Stage: Verify JAR -> if exist + dir'
                bat 'if exist target\\hotel-system-1.0-SNAPSHOT.jar (echo [VERIFY OK] JAR found: target\\hotel-system-1.0-SNAPSHOT.jar & dir target\\*.jar) else (echo [VERIFY FAIL] JAR not found: target\\hotel-system-1.0-SNAPSHOT.jar & exit /b 1)'
            }
        }
    }
}