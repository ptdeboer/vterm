/* tscript.c
   2     This program illustrates an implementation of the concept of pseudo
   3     terminals on a single machine. It works like the typical UNIX script
   4     program.

   5     There are five functions in this program:
   6       main():
   7       . Check to see whether the user has entered a script filename.
   8         If yes, use it, otherwise use the default (typescript)
   9       . Save the current terminal attributes for stdin.
  10       . Set up a signal handler, catch_child(), for SIGCHLD.
  11       . Fork a subshell
  12         In the child process, close master fd, execute runshell().
  13         In the parent process, close slave fd, execute script().

  14       catch_child(): the signal handler for SIGCHLD
  15       . Reset terminal attributes for stdin.
  16       . Terminate calling process.

  17       runshell(): executes a subshell for the slave terminal
  18       . Call setpgrp() to have the shell runs in its own process group.
  19         So the shell will have full control over signal handling.
  20       . Invoke dup2() to set stdin, stdout and stderr to reference the
  21         slave file descriptor. This is a critical step.
  22       . Call exec() to execute a subshell until terminated by the user.

  23       pttyopen() is used to open the pseudo tty
  24       . Open the pseudo tty to obtain fd for the master pseudo tty.
  25       . Change the permissions of the slave pseudo tty.
  26       . Unlock the slave device associated with master fd.
  27       . Retrieve the name of the slave pseudo tty and open it.
  28       . Construct the line discipline.

  29       script(): changes the line discipline and echoes user input
  30       . Change the line discipline to raw mode.
  31       . Open the tscript output file.
  32       . Call select() with no timeout to check for input from stdin and
  33         master pseudo device.
  34         Echo input from stdin to master pseudo device, and input from master
  35         pseudo device to both the script file and stdout.

  36     Note: This example is derived from the tscript program introduced in
  37     UNIX System Programming, Haviland & Salama, Prentice Hall, 1999.
  38  */

/* 
 * (C) Public Domain code.
 *
 * ---
 *
 * Modifications by Piter T. de Boer:
 *  - loading of modules "ptem" and "ldterm" are optional.
 *    When loading fails, continue anyway
 *  - disabled 'script'-like copying of stdout to file.
 *  - added option: '-h' '-w' options for initial size of rows, columns respectively.
  */

#include <stropts.h>
#include <sys/types.h> 
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <termio.h>
#include <signal.h>
#include <string.h> 

void catch_child(int);
void runshell(int);
void script(int, char *);
int pttyopen(int *, int *);
// mine:
void sendSize(int *slavefd, short unsigned int rows, short unsigned int cols);

struct termios dattr;

// defaults:
int scriptout=0;
const char *shellcmd="/bin/bash";
short unsigned int nr_rows=25;
short unsigned int nr_columns=80;

/**
 * main
 */
int main(int argc, char *argv[])
{
  struct sigaction action;
  int  mfd, sfd;
  char *scriptname;
  int i=0;

  for (i=1;i<argc;i++)
  {
    if (argv[i][0]=='-')
    {
        if (i+1<argc)
        {
    	   if (strcmp(argv[i],"-o")==0)
    	   {
    	      scriptname=strdup(argv[i+1]);
    	      scriptout=1;
              i++;
              continue; 
    	   }
    	   else if (strcmp(argv[i],"-sh")==0)
    	   {
    	      shellcmd=strdup(argv[i+1]);
              i++; 
              continue; 
    	   }
     	   else if (strcmp(argv[i],"-h")==0)
           {
              nr_rows=atoi(argv[i+1]);
              i++;
              continue;
           }
           else if (strcmp(argv[i],"-w")==0)
           {
              nr_columns=atoi(argv[i+1]);
              i++;
              continue;
           }
    	   else
    	   {
    	      fprintf(stderr,"%s: unknown argument: %s\n",argv[0],argv[i]);
    	   }
    	}
    	else
    	{
    	    fprintf(stderr,"%s: unknown argument: %s\n",argv[0],argv[i]);
    	}
    }
  }// for

  tcgetattr(STDIN_FILENO, &dattr);   /* save the current terminal settings */

  if (pttyopen(&mfd, &sfd) == -1) {  /* open both master & slave */
      perror("opening pseudo tty");
      exit(1);
  }

  /* set up the action to be taken on receipt of SIGCHLD */
  action.sa_handler = catch_child;
  sigfillset(&action.sa_mask);
  sigaction(SIGCHLD, &action, (struct sigaction *)NULL);

  /* create the shell process */
  switch(fork())
  {
    case -1:                  /* error */
      perror("fork failed on shell");
      exit(2);
    case 0:                   /* child */
      close(mfd);
      runshell(sfd);
    default:                  /* parent */
      close(sfd);
      script(mfd, scriptname);
  }
  return 0;
}  /* main */

/**
 *  catch_child(): the signal handler for SIGCHLD
 */
void catch_child(int signo)
{
  tcsetattr(STDIN_FILENO, TCSAFLUSH, &dattr);
  exit(0);
}  /* catch_child */

/**
 *  pttyopen() is used to open the pseudo tty
 */
int pttyopen(int *masterfd, int *slavefd)
{
  char *slavenm;

  /* open the pseudo tty to obtain fd for the master ptty */
  if ((*masterfd = open("/dev/ptmx", O_RDWR)) == -1)
  {
      perror("masterfd.open");
      return(-1);
  }

  /* change the permissions of the slave pseudo tty
     . change the ownership of the slave to the effective user ID
     . change the group ownership to the group tty
     . change the permissions to allow rw- -w- ---
  */

  if (grantpt(*masterfd) == -1)
  { /* grant access to slave PT device */
      perror("masterfd.grantpt");
      close(*masterfd);
      return(-1);
  }

  /* unlock the slave device associated with master fd */
  if (unlockpt(*masterfd) == -1)
  {
      perror("masterfd.unlockpt");
      close(*masterfd);
      return(-1);
  }

  /* retrieve the name of the slave ptty and open it */
  if ((slavenm = ptsname(*masterfd)) == NULL)
  {
      perror("masterfd.ptsname");
      close(*masterfd);
      return(-1);
  }

  fprintf(stderr,"slavename=%s\n",slavenm);

  if ((*slavefd = open(slavenm, O_RDWR)) == -1)
  {
      perror("slavefd.open");
      close(*slavefd);
      return(-1);
  }

  /* construct the line discipline for slave
     . ptem is pseudo-terminal emulation module
     . ldterm is terminal line discipline module
     . These two modules together act like a real terminal
  */

  if (ioctl(*slavefd, I_PUSH, "ptem") == -1)
  {
      // Piter T. de Boer: happens when binary is started from command line shell which is already 
      // asociated with a pty. Ignore.
      perror("slavefd.ioctl(\"ptem\")");
      //close(*masterfd);
      //close(*slavefd);
      //return(-1);
  }

  if (ioctl(*slavefd, I_PUSH, "ldterm") == -1)
   {
      // Piter T. de Boer: happens when binary is started from command line shell. Ignore.
      perror("slavefd.ioctl(\"ldterm\")");
      //close(*masterfd);
      //close(*slavefd);
      //return(-1);
  }

  // works ! but now catch resizes !
  sendSize(slavefd,nr_rows,nr_columns);

  return(1);
}  /* pttyopen */

/**
 * Send size
 */
void sendSize(int *slavefd, short unsigned int rows, short unsigned int cols) {

// *        struct winsize {
// *               unsigned short ws_row;
// *               unsigned short ws_col;
// *               unsigned short ws_xpixel;   /* unused */
// *               unsigned short ws_ypixel;   /* unused */
// *           };

  struct winsize ws;
  ws.ws_row=rows;
  ws.ws_col=cols;

  if (ioctl(*slavefd, TIOCSWINSZ, &ws) == -1) {
      perror("slavefd.ioctl(TIOCSWINSZ)");
  }

}

/**
 *  runshell(): executes a subshell for the slave terminal
 */
void runshell(int sfd)
{
  setpgrp();

  dup2(sfd, 0);
  dup2(sfd, 1);
  dup2(sfd, 2);

  execl(shellcmd,shellcmd, "-i", (char *)0);
}  /* runshell */

/**
 *  script(): changes the line discipline and echoes user input
 */
void script(int mfd, char *scriptname)
{
  int nread, ofile=-1;
  fd_set set, master;
  struct termios attr;
  char buf[BUFSIZ];

  /* change the line discipline to raw mode */
  tcgetattr(STDIN_FILENO, &attr);
  attr.c_cc[VMIN] = 1;
  attr.c_cc[VTIME] = 0;
  attr.c_lflag &= ~(ISIG | ECHO | ICANON);
  tcsetattr(STDIN_FILENO, TCSAFLUSH, &attr);

  /* open the tscript output file - assume no error */
  if (scriptout==1)
     ofile = open(scriptname, O_CREAT | O_WRONLY | O_TRUNC, 0666);

  /* set the bit masks for the select system call */
  FD_ZERO(&master);
  FD_SET(STDIN_FILENO, &master);
  FD_SET(mfd, &master);

  /* select() is called with no timeout, it may wait forever */
  while (set = master, select(mfd+1, &set, NULL, NULL, NULL) > 0)
  {
      /* check the standard input */
      if (FD_ISSET(STDIN_FILENO, &set))
      {
          nread = read(STDIN_FILENO, buf, BUFSIZ);
          write(mfd, buf, nread);
          #ifdef DEBUG
              fprintf(stderr,"> stdin -> pty: num read:%i\n",nread);
              fprintf(stderr,"> stdin -> pty: ");
              for (int i=0;i<nread; i++) {
                fprintf(stderr,"%02x ",buf[i]);
              }
              fprintf(stderr,"\n");
          #endif
      }

      /* check the master pseudo device */
      if (FD_ISSET(mfd, &set))
      {
          nread = read(mfd, buf, BUFSIZ);
          if (ofile>=0)
              write(ofile, buf, nread);
          write(STDOUT_FILENO, buf, nread);
          #ifdef DEBUG
             fprintf(stderr,"< stdout <- pty: num read:%i\n",nread);
             fprintf(stderr,"< stdout <- pty: ");
             for (int i=0;i<nread; i++) {
                fprintf (stderr,"%02x ",buf[i]);
             }
             fprintf(stderr,"\n");
          #endif
      }
  }

}  /* script */

