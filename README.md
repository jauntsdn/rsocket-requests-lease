[![Build Status](https://travis-ci.org/jauntsdn/rsocket-requests-lease.svg?branch=develop)](https://travis-ci.org/jauntsdn/rsocket-requests-lease)

### RSocket-requests-lease

https://jauntsdn.com/post/rsocket-lease-concurrency-limiting/

Limiting service latency with concurrency control by RSocket requests lease.  
Application consists of single RSocket-RPC client connecting set of RSocket-RPC servers via reverse proxy.

1. Start servers
`./lease_server.sh localhost 8309`  
`./lease_server.sh localhost 8310`  
`./lease_server.sh localhost 8311`  

2. Start proxy
`./lease_proxy.sh localhost 8308 localhost:8309,localhost:8310,localhost:8311`

3. Start client
`./lease_client.sh localhost 8308`

## License
Copyright 2020 - Present Maksym Ostroverkhov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
