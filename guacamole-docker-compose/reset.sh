#!/bin/bash
echo "This will delete your existing database (./data/)"
echo "This will delete your existing containers (guacamole guacd postgres)"
echo ""
read -p "Are you sure? " -n 1 -r
echo ""   # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then # do dangerous stuff
 docker-compose down
 sudo rm -r -f ./data/  ./init/
fi


