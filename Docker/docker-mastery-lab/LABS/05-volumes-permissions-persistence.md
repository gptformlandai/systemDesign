# Lab 05: Volumes, Permissions, And Persistence

## Goal

See why container writable layers are disposable and how volumes preserve state.

## Named Volume Drill

```bash
docker volume create docker-mastery-data
docker run --rm -v docker-mastery-data:/data alpine sh -c 'date > /data/created.txt'
docker run --rm -v docker-mastery-data:/data alpine cat /data/created.txt
docker volume inspect docker-mastery-data
```

Cleanup when done:

```bash
docker volume rm docker-mastery-data
```

## Writable Layer Drill

```bash
docker run --name disposable alpine sh -c 'date > /tmp/lost.txt'
docker rm disposable
```

The file is gone because it lived only in the container writable layer.

## Permission Drill

Use a bind mount and compare host ownership with container user:

```bash
mkdir -p ./tmp-bind
docker run --rm -v "$PWD/tmp-bind:/data" alpine id
docker run --rm -v "$PWD/tmp-bind:/data" alpine sh -c 'id && touch /data/test.txt && ls -la /data'
```

## Interview Takeaway

```text
Docker storage debugging starts by identifying writable layer, named volume, bind mount, or external storage. Then I check mount path, lifecycle, ownership, and backup expectations.
```