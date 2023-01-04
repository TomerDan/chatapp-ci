from datetime import datetime
from flask import Flask, request, render_template
import mysql.connector


app = Flask(__name__,template_folder='templat')

#DB
mass=[]
mydb = mysql.connector.connect(
  host="mysql",
  port= "3306",
  user="root",
  password="root",
  database="mydb"
)
mycursor = mydb.cursor()

def add_msg(room) :
    UESR=request.values['username']
    MESSAGE=request.values['msg']
    now = datetime.now()
    mass.append([[room],[str(now.strftime("%d/%m/%Y %H:%M:%S ") + "[" + UESR + "]" + " : " + MESSAGE + "\n")]])
    sql = "INSERT INTO msg_table (usernamedb, msgdb) VALUES (%s, %s)"
    msg_to_str = str(now.strftime("%d/%m/%Y %H:%M:%S ")) + "[" + UESR + "]" + " : " + MESSAGE
    room_to_str = str(room)
    val = (f"{room_to_str}", f"{msg_to_str}")
    mycursor.execute(sql, val)
#create collection with a room Id and a messages array
def find_msg_for_room(room) :
    content = ""
    sql = f"SELECT usernamedb,msgdb FROM msg_table WHERE usernamedb ={room}"
    mycursor.execute(sql)
    myresult = mycursor.fetchall()
    for x in myresult :
        content += "\n".join(x)
    return content
#create collection with a "general" Id and a messages array
def msg_general_room ():
    content = ""
    mycursor.execute("SELECT usernamedb,msgdb FROM msg_table")
    myresult = mycursor.fetchall()
    for x in myresult :
        content += "\n".join(x)
    return content

#home[GET]
@app.route('/')
def home():
   return render_template('home.html')

#home[GET]
@app.route('/<room>')
def chat(room):
    return render_template('home.html')

#general/room[POST]
@app.route('/api/chat/<room>', methods=["POST", "GET"])
def index(room):
    if request.method == 'GET':
        if room == 'general':
            return msg_general_room()
        else :
            return find_msg_for_room(room)
    elif request.method == 'POST':
        add_msg(room)
        return "1"



if __name__ == '__main__':
   app.run(host='0.0.0.0',port=8083,debug=True)
