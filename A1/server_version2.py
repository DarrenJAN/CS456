from socket import * 
from sys import *
import string  

#find free port number
def find_free_port():
  tmp_socket = socket(AF_INET, SOCK_STREAM)
  tmp_socket.bind(('',0))
  return tmp_socket.getsockname()[1]

def server_TCP(server_req):
  r_port = find_free_port() #get the free port number
  serverSocket = socket(AF_INET, SOCK_STREAM) #create server socket
  serverSocket.bind(('',r_port))
  print("SERVER_PORT=")
  print(serverSocket.getsockname()[1])

  #server socket wait from client socket
  serverSocket.listen(1)
  print("Now server is ready to receive")
  while True:
    connectionSocket,addr = serverSocket.accept()
    req_code = connectionSocket.recv(1024).decode()
    
    #once the server verifies the req_code, it replies back with the r_port
    if(server_req == req_code):
      connectionSocket.send(str(serverSocket.getsockname()[1]))
      UDP_socket = socket(AF_INET, SOCK_DGRAM)
      UDP_socket.bind(('',r_port))
      break
    #if the req_code is not matched, the server closes the TCP connnection 
    else:
      print("Wrong Request code")
      serverSocket.close()
      
    connectionSocket.close()

  return UDP_socket


def server_UDP(server_req):
    serverSocket = server_TCP(server_req)
    print("The server is ready to receive the message")
    message, clientAddress =  serverSocket.recvfrom(2048)
    modified_msg = message.decode()
    modified_msg = modified_msg[::-1]
    serverSocket.sendto(modified_msg,clientAddress)
    serverSocket.close()

if __name__ == "__main__":
  if (len(argv)  == 2):
     while True:
        req_code = argv[1]
        #serverSocket = server_TCP((req_code))
        #server_port = serverSocket.getsockname()[1]
        server_UDP(req_code)
  else:
    print("Wrong input")
  


