#!/bin/sh
echo "[*] Uninstalling Bitcoin and Tuskex, will delete all data!!"
sleep 10
sudo rm -rf /root/tuskex
sudo systemctl stop bitcoin
sudo systemctl stop tuskex-seednode
sudo systemctl disable bitcoin
sudo systemctl disable tuskex-seednode
sudo userdel -f -r tuskex
sudo userdel -f -r bitcoin
echo "[*] Done!"
