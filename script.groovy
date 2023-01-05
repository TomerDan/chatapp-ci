def version(){                  
    
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "G_USER", passwordVariable: "G_PASS"]]) {
    sh "git fetch https://${G_USER}:${G_PASS}@${env.GIT_URL_HTTP} --tags"
    }
    majorMinor = sh(script: "git tag -l --sort=v:refname | tail -1 | cut -c 1-3", returnStdout: true).trim()
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
    docker compose up -d
    '''  
}

def test(){
    sh'''
    docker network connect ubuntu_default app
    curl app:8083
    '''
}

def tag(){                    
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "G_USER", passwordVariable: "G_PASS"]]) {
        sh "git config --global user.email 'daniely.tomer@gmail.com'"
        sh "git config --global user.name 'tomer'"
        sh "git clean -f -x"
        sh "git tag -a ${env.VERSION} -m 'version ${env.VERSION}'"
        sh "git push https://${G_USER}:${G_PASS}@${env.GIT_URL_HTTP} --tag"
    }}
    
def build_for_ecr(){
    app = docker.build("812619807720.dkr.ecr.us-east-1.amazonaws.com/app","-f ./app/Dockerfile ./app/")
}

def publish_image(){                       
    docker.withRegistry('https://812619807720.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:aws_ecr') {
    app = docker.build("812619807720.dkr.ecr.us-east-1.amazonaws.com/app","-f ./app/Dockerfile ./app/")
    app.push("${env.VERSION}")
    } }


def clean_docker(){
    sh 'docker-compose down --remove-orphans --volumes'
}

def update_helm_version(){
        withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "G_USER", passwordVariable: "G_PASS"]]) {
        sh "git clone https://${G_USER}:${G_PASS}@github.com/TomerDan/chatapp-helm.git"
        sh "git config --global user.email 'daniely.tomer@gmail.com'"
        sh "git config --global user.name 'tomer'"
        dir('chatapp-helm') {
            sh "cat chatapp/values.yaml"
            sh "sed -i 's/tag: .*/tag: ${env.VERSION}/g' chatapp/values.yaml"
            sh "cat chatapp/values.yaml"
            sh "git add ."
            sh "git commit -m 'push version from jenkins'"
            sh "git push https://${G_USER}:${G_PASS}@github.com/TomerDan/chatapp-helm.git HEAD:main"
            //sh "git tag -a ${env.VERSION} -m 'version ${env.VERSION}'"
            //sh "git push https://${G_USER}:${G_PASS}@github.com/TomerDan/chatapp-helm.git --tag"
    }}}
return this