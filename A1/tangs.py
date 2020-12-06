from socket import *
import sys
import string
import random

def TCP_Init(req_code):
    ## create a socket to listen request with random port number
    r_port = random.randrange(1025,65536)
    server_socket = socket(AF_INET, SOCK_STREAM)
    server_socket.bind(('', r_port))
    print("SERVER_PORT=" + str(server_socket.getsockname()[1]))
    
    ## begin to listen request from client
    server_socket.listen(1)
    print("The server is ready to receive!")
    while True:
        connectionSocket, addr = server_socket.accept()
        command_code = connectionSocket.recv(1024)
        if command_code == req_code:
            ## r_socket is for UDP transaction use
            r_socket = socket(AF_INET, SOCK_DGRAM)
            r_socket.bind(('', r_port))
            connectionSocket.send(str(r_socket.getsockname()[1]))
            break
        else:
            print("Oops!! Invalid request code")
            sys.exit() 
        
        connectionSocket.close()
    return r_socket


def UDP_Trans(req_code):
    while True:
        server_socket = TCP_Init(req_code)
        msg, client_ip = server_socket.recvfrom(2048)
        reversed_msg = msg[::-1]
        server_socket.sendto(reversed_msg, client_ip)
        server_socket.close()

if __name__ == "__main__":
    req_code = sys.argv[1]
    UDP_Trans(req_code)