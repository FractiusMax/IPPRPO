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
        stage('Checkout') {
            steps {
                echo 'Stage: Checkout -> copy Maven project into workspace'
                bat '''
                    if not exist "C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" (
                        echo [CHECKOUT FATAL] mvn.cmd ne nayden
                        exit /b 1
                    )
                    if not exist "D:\\bbgg\\hotel\\pom.xml" (
                        echo [CHECKOUT FATAL] net pom.xml v D:\\bbgg\\hotel
                        exit /b 1
                    )
                    if exist src rmdir /S /Q src
                    if exist target rmdir /S /Q target
                    copy /Y "D:\\bbgg\\hotel\\pom.xml" pom.xml > NUL
                    xcopy /E /I /Q /Y "D:\\bbgg\\hotel\\src" src > NUL
                    echo [CHECKOUT OK] proekt skopirovan v workspace
                    dir /B
                '''
            }
        }

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