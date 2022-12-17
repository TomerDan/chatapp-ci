def version(){                  
    majorMinor = env.BRANCH_NAME.split("/")[1]  // x.y
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "GL_USER", passwordVariable: "GL_PASS"]]) {
    sh "git fetch https://${GL_USER}:${GL_PASS}@${env.GIT_URL_HTTP} --tags"
    }
    previousTag = sh(script: "git describe --tags --abbrev=0 | grep -E '^$majorMinor' || true", returnStdout: true).trim()  // x.y.z or empty string. `grep` is used to prevent returning a tag from another release branch; `true` is used to not fail the pipeline if grep returns nothing.
    if (!previousTag) {
    patch = "0"
    } else {
    patch = (previousTag.tokenize(".")[2].toInteger() + 1).toString()
    }
    env.VERSION = majorMinor + "." + patch
    echo "env.version"
    echo "env.BRANCH_NAME"               
}

def build(){
    sh'''
    docker system prune -a --volumes -f
    docker build ./app/ -f ./app/Dockerfile -t app
    docker-compose up -d
    '''  
}

def test(){
    sh'''
    docker network connect lab_default app
    curl http://app:8083/
    '''
}

def tag(){                    
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "G_USER", passwordVariable: "G_PASS"]]) {
        sh "git config --global user.email 'daniely.tomer@gmail.com'"
        sh "git config --global user.name 'tomer'"
        sh "git clean -f -x"
        sh "git tag -a ${env.VERSION} -m 'version ${env.VERSION}'"
        sh "git push http://${G_USER}:${G_PASS}@${env.GIT_URL_HTTP} --tag"
    }}

def publish_ecr(){         
    configFileProvider(
    [configFile(fileId: '795c8c4a-8d86-4326-90c7-8577ce9849ae', variable: 'MAVEN_SETTINGS')]) {
    sh 'mvn -s $MAVEN_SETTINGS deploy -DskipTests'
    }}

def clean_docker(){
    sh 'docker-compose down --remove-orphans --volumes'
}

