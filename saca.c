#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include <malloc.h>
#include <pthread.h>
#include "saca.h"
#define PORT 2433
#define PORTC 2401

int planesSockets[300];
int controlsSockets[300];
int planeCount = 0;
int contCompteur = 0;
int readFromPLanes;
int ecouterCont;
char **nomAvions = 0;
//struct avion avions[300];

void listenToPLanesFunc(void *param)
{
  char buffer[1024] = {0};
  while(1)
  {
    for(int i=0; i< planeCount; i++)
    {
      readFromPLanes = read(planesSockets[i],  buffer, 1024);
      if(readFromPLanes > 0)
      {
        
        printf("%s\n",buffer );
/*printf(donnee, "Avion %s -> localisation : (%d,%d), altitude : %d, vitesse : %d, cap : %d\n",
            numero_vol, coord.x, coord.y, coord.altitude, dep.vitesse, dep.cap);*/
      }
    }
  } 
}


void connecter_avions(void *param){

    
    int server_fd, new_socket, valread;
    struct sockaddr_in address;
    int opt = 1;
    int addrlen = sizeof(address);
    char buffer[1024] = {0};
    pthread_t listenToPLanes;


    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }
      
   /* if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT,
                                                  &opt, sizeof(opt)))
    {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }*/
    


    address.sin_family = AF_INET;
    address.sin_addr.s_addr = inet_addr("127.0.0.1");
    address.sin_port = htons( PORT );

    if (bind(server_fd, (struct sockaddr *)&address, 
                                 sizeof(address))<0)
    {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }
    if (listen(server_fd, 300) < 0)
    {
        perror("listen");
        exit(EXIT_FAILURE);
    }
    char *x = (char*)malloc(sizeof(char)*5);
    char *y = (char*)malloc(sizeof(char)*5);
    char *a = (char*)malloc(sizeof(char)*5);
    char *c = (char*)malloc(sizeof(char)*5);
    char *v = (char*)malloc(sizeof(char)*5);
    int xx;
    int comaCounter = 0;
    int counter = 0;
    char *bufferx;
    while(1)
    {
	if ((new_socket = accept(server_fd, (struct sockaddr *)&address,(socklen_t*)&addrlen))<0)
	{
	  perror("accept");
	  exit(EXIT_FAILURE);
	}
	planesSockets[planeCount] = new_socket;
        read(new_socket,  buffer, 1024); 
        printf("%s\n",buffer);
        nomAvions[planeCount] = (char*)malloc(sizeof(char)*5);
        strncpy( nomAvions[planeCount], (char*)buffer, 5);

        for(int i=5; i< strlen(buffer); i++)
        {
          
  
          if(buffer[i] == ',')
          {
            comaCounter++;
            counter = 0;
          }
          else 
  	  {
	    if(comaCounter == 1)
            {
	      x[counter] = buffer[i]; 
	    }
            else if (comaCounter == 2)
	    {
	      y[counter] = buffer[i]; 
	    }   
	    else if (comaCounter == 3)
	    {
	      a[counter] = buffer[i]; 
            }  
	    else if (comaCounter == 4)
	    {
	      v[counter] = buffer[i]; 
	    }   
	    else if (comaCounter == 5)
	    {
	      c[counter] = buffer[i]; 
	    } 
            counter++;   
          }
         //printf(buffer[i]);
        }
        printf("%s - %s - %s - %s - %s -\n",x,y,a,v,c);
       xx = atoi(x);
    
        printf("%i\n",xx);      
        
       /* if (planeCount == 0 )
        {
          pthread_create(&listenToPLanes, NULL, listenToPLanesFunc, NULL); 
        }*/
        planeCount++;
    }
}

void ecouterControlleur(void *param)
{
  char buffer[1024] = {0};
  while(1)
  {
    for(int i=0; i< contCompteur; i++)
    {
      ecouterCont = read(controlsSockets[i],  buffer, 1024);
      if(ecouterCont > 0)
      {
        printf("%s\n",buffer );
      }
    }
  } 
}

void connecter_controlleurs(void *param){
    
    int server_fd, new_socket, valread;
    struct sockaddr_in address;
    int opt = 1;
    int addrlen = sizeof(address);
    char buffer[1024] = {0};
    pthread_t ecouter_controlleurs;


    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0)
    {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }
      
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = inet_addr("127.0.0.1");
    address.sin_port = htons( PORTC );

    if (bind(server_fd, (struct sockaddr *)&address, 
                                 sizeof(address))<0)
    {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, 300) < 0)
    {
        perror("listen");
        exit(EXIT_FAILURE);
    }
   
    while(1)
    {
	if ((new_socket = accept(server_fd, (struct sockaddr *)&address,(socklen_t*)&addrlen))<0)
	{
	  perror("accept");
	  exit(EXIT_FAILURE);
	}
	controlsSockets [contCompteur] = new_socket;
	//
        if (contCompteur == 0 )
        {
          pthread_create(&ecouter_controlleurs, NULL, ecouterControlleur, NULL); 
        }
        contCompteur++;
    }
}

int main(int argc, char const *argv[])
{   
  pthread_t connectAvions;
  pthread_create(&connectAvions, NULL, connecter_avions, NULL);
  nomAvions = (char**)malloc(sizeof(char*)*300);
  pthread_t connectControlleurs;
  pthread_create(&connectControlleurs, NULL, connecter_controlleurs, NULL);
  
  getchar();
  return 0;


}
