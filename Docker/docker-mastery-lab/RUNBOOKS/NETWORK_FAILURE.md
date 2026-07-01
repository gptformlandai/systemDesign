# Runbook: Docker Network Failure

## Symptoms

- host cannot reach published service
- container cannot reach another container
- DNS name does not resolve

## Evidence Commands

```bash
docker ps
docker port CONTAINER
docker inspect CONTAINER --format '{{json .NetworkSettings.Networks}}'
docker network ls
docker network inspect NETWORK
docker logs CONTAINER --tail 100
```

## Check

- traffic direction
- published host port
- container listen address
- service name vs localhost
- user-defined network membership
- host firewall or proxy

## Mitigate

- publish correct port
- bind app to `0.0.0.0`
- attach services to same network
- use service names instead of container IPs
- rollback config if network changed in deploy

## Prevent

- document ports and traffic paths
- use Compose service names locally
- add health checks and smoke tests