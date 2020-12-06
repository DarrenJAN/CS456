from socket import *
import string
import sys

def TCP_Init(sever_ip, n_port, req_code):
    ## first connection, use fixed port (n_port)
    clientSocket = socket(AF_INET, SOCK_STREAM)
    clientSocket.connect((sever_ip, n_port))
    clientSocket.send(req_code)
    ## receive random port (r_port) for next connection use
    r_port = clientSocket.recv(1024)
    clientSocket.close()
    return int(r_port)


def UDP_Trans(sever_ip, r_port, msg):
    clientSocket = socket(AF_INET, SOCK_DGRAM)
    ## send request using random port
    clientSocket.sendto(msg.encode(), (sever_ip, r_port))
    reversed_msg, sever_ip = clientSocket.recvfrom(2048)
    clientSocket.close()
    return reversed_msg

if __name__ == "__main__":
    sever_ip = sys.argv[1]
    n_port = int(sys.argv[2])
    req_code = sys.argv[3]
    msg = sys.argv[4]

    r_port = TCP_Init(sever_ip, n_port, req_code)
    reversed_msg = UDP_Trans(sever_ip, r_port, msg)

    print(reversed_msg)