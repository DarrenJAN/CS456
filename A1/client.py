from socket import *
from sys import *

def client_TCP(serverAddress, serverPort,req_code):
  #create TCP socket for client
  clientSocket = socket(AF_INET, SOCK_STREAM)
  clientSocket.connect((serverAddress, serverPort))
  #client socket send the req_code
  clientSocket.send(req_code)
  #client receives the r_port number from server 
  r_port = clientSocket.recv(1024)
  #once client receive r_port, then close the TCP connection
  if (r_port  == serverPort):
    clientSocket.close() 

def client_UDP(msg, serverAddress, serverPort):
  #create UDP socket for client 
  clientSocket = socket(AF_INET, SOCK_DGRAM)
  clientSocket.sendto(msg.encode(), (serverAddress,int(serverPort)))
  #receive modified message from server
  modified_msg, serverAddress= clientSocket.recvfrom(2048)
  clientSocket.close()
  return modified_msg


if __name__ == "__main__":
  if(len(argv) == 5):
    serverAddress = argv[1]
    n_port = argv[2]
    req_code = argv[3]
    message = argv[4]
    client_TCP(serverAddress,int( n_port),req_code)
    modified_msg = client_UDP(message,serverAddress,n_port)
    print(modified_msg)
  else:
    print("Wrong input")
    
  exit()

