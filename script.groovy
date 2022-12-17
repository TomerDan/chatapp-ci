def version(){                  
    majorMinor = env.BRANCH_NAME.split("/")[1]  // x.y
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: "github-protfolio", usernameVariable: "G_USER", passwordVariable: "G_PASS"]]) {
    sh "git fetch https://${G_USER}:${G_PASS}@${env.GIT_URL_HTTP} --tags"
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
    curl app:8083
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
    
def build_for_ecr(){
    app = docker.build("644435390668.dkr.ecr.us-east-1.amazonaws.com/tomer-protfolio","-f ./app/Dockerfile ./app/")
}

def publish_image(){                 
    docker.withRegistry('https://644435390668.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:aws_protfolio_ecr') {
    app = docker.build("644435390668.dkr.ecr.us-east-1.amazonaws.com/tomer-protfolio","-f ./app/Dockerfile ./app/")
    app.push()
    } }


def clean_docker(){
    sh 'docker-compose down --remove-orphans --volumes'
}

return this