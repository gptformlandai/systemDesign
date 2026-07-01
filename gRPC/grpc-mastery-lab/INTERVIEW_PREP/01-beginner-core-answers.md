# Beginner Core Answers

## What Is gRPC?

gRPC is a typed RPC framework. It commonly uses Protocol Buffers for contracts and HTTP/2 for transport, and it generates client and server code from `.proto` files.

## What Is Protobuf?

Protocol Buffers define typed messages with stable field numbers. They are compact on the wire and support schema evolution when used carefully.

## What Is A Stub?

A stub is generated code that lets client and server code use typed RPC methods without hand-writing low-level serialization and transport code.

## What Are RPC Types?

Unary, server streaming, client streaming, and bidirectional streaming.

## Strong Beginner Close

The proto is the contract, generated stubs are the boundary code, and HTTP/2 carries the RPC call.