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
        // Checkout — выполняется SCM-плагином Jenkins (Pipeline script from SCM):
        // Job забирает код из Git-репозитория по указанному Repository URL
        // и ветке, после чего Jenkinsfile берётся из корня этого checkout.
        stage('Checkout') {
            steps {
                echo 'Stage: Checkout -> SCM plugin (Pipeline script from SCM)'
                echo "GIT_URL    = ${env.GIT_URL}"
                echo "GIT_BRANCH = ${env.GIT_BRANCH}"
                echo "GIT_COMMIT = ${env.GIT_COMMIT}"
                bat 'chcp 65001 >nul & dir /B'
            }
        }

        stage('Build') {
            steps {
                echo 'Stage: Build -> mvn clean compile'
                bat 'chcp 65001 >nul & "C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B -DskipTests clean compile'
            }
        }

        // ЛР №5. Отдельный этап статического анализа.
        // Команда соответствует настройке maven-checkstyle-plugin в pom.xml:
        // goal check использует файл правил checkstyle.xml и завершает сборку
        // с ошибкой при обнаружении нарушений.
        stage('Static Analysis') {
            steps {
                echo 'Stage: Static Analysis -> mvn checkstyle:check'
                bat 'chcp 65001 >nul & "C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B checkstyle:check'
            }
        }

        stage('Test') {
            steps {
                echo 'Stage: Test -> mvn test'
                bat 'chcp 65001 >nul & "C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B test'
            }
        }

        stage('Package') {
            steps {
                echo 'Stage: Package -> mvn package -DskipTests'
                bat 'chcp 65001 >nul & "C:\\Program Files\\Apache\\Maven\\apache-maven-3.9.16\\bin\\mvn.cmd" -B package -DskipTests'
            }
        }

        stage('Verify JAR') {
            steps {
                echo 'Stage: Verify JAR -> if exist + dir'
                bat 'chcp 65001 >nul & if exist target\\hotel-system-1.0-SNAPSHOT.jar (echo [VERIFY OK] JAR found: target\\hotel-system-1.0-SNAPSHOT.jar & dir target\\*.jar) else (echo [VERIFY FAIL] JAR not found: target\\hotel-system-1.0-SNAPSHOT.jar & exit /b 1)'
            }
        }
    }
}
