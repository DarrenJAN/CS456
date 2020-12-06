from socket import * 
from sys import *
import string  

#find free port number
def find_free_port():
  tmp_socket = socket(AF_INET, SOCK_STREAM)
  tmp_socket.bind(('',0))
  return tmp_socket.getsockname()[1]

def server_TCP(server_req):
  #get the free port number 
  r_port = find_free_port()
  #create TCP socket
  serverSocket = socket(AF_INET, SOCK_STREAM)
  serverSocket.bind(('',r_port))
  print("SERVER_PORT=" + str(serverSocket.getsockname()[1]))
  #now server socket wait from client's connection 
  serverSocket.listen(1)
  while True:
    #client send a req_code to request to get the r_port number
    connectionSocket,addr = serverSocket.accept()
    req_code = connectionSocket.recv(1024).decode()
    #once the server verifies the req_code, it replies back with the r_port
    if(server_req == req_code):
      connectionSocket.send(str(serverSocket.getsockname()[1]))
      break
    #if the req_code is not matched, the server closes the TCP connnection 
    else:
      serverSocket.close()
    connectionSocket.close()
  return r_port


def server_UDP(server_port):
  #create UDP server socket
  serverSocket = socket(AF_INET, SOCK_DGRAM);
  serverSocket.bind(('',server_port))
  #now wait for receiving the message and reverse it 
  message, clientAddress =  serverSocket.recvfrom(2048)
  modified_msg = message.decode()
  modified_msg = modified_msg[::-1]
  #send back to client socket and close the connection 
  serverSocket.sendto(modified_msg,clientAddress)
  serverSocket.close()

if __name__ == "__main__":
  if (len(argv)  == 2):
      while True:
        req_code = argv[1]
        server_port= server_TCP((req_code))
        server_UDP(server_port)
  else:
    print("Wrong input")
  



