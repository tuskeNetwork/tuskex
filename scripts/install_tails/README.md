# Steps to use (This has serious security concerns to tails threat model only run when you need to access tuskex)

## 1. Enable persistent storage and admin password before starting tails

## 2. Get your tuskex deb file in persistent storage, currently most people use tuskex-reto (amd64 version for tails)

## 3. Edit the path to the tuskex deb file if necessary then run ```sudo ./tuskex-install.sh```
## 4. As amnesia run ```source ~/.bashrc``` 
## 5. Start tuskex using ```tuskex-tails```

## You will need to run this script after each reset, but your data will be saved persistently in /home/amnesia/Persistence/Tuskex-reto
