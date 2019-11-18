
SHARED=$1
#echo $SHARED

sudo ifconfig enp7s0:1 192.168.184.1/2
sudo iptables -A FORWARD -o eth0 -i enp7s0:1 -s 192.168.184.0/24 -m conntrack --ctstate NEW -j ACCEPT
sudo iptables -A FORWARD -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
sudo iptables -t nat -F POSTROUTING
sudo iptables -t nat -A POSTROUTING -o $SHARED -j MASQUERADE
