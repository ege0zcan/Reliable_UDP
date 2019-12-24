# Reliable_UDP
Achieving reliable  data transfer over UDP by utilizing selective repeat mechanism in the application layer. 
Assignment for Computer Networks course. The receiver code was not written by me.

## Summary
Every packet in the transmission window is handled by a new thread. A thread sends the packet and starts waiting. 
If the main process receives an ACK message, it interrupts the thread. 
If the retransmission timer expires before an interrupt the packet is retransmitted. 

## Note
To simulate packet losses and transmission delays `packet_loss_probability` and `max_packet_delay` arguments are used in the receiver.
The receiver drops the data packets with the specified probability p, and it delays the reception of the data packets with the 
amount of delay randomly chosen from the interval [0, Dmax]. This emulates the effects of network delay. 


## How to run

First start the receiver from the console:

`python receiver.py <bind_port> <window_size_N> <packet_loss_probability_p> <max_packet_delay_Dmax>`

Then run the client code: 

`java Sender <file_path> <receiver_port> <window_size_N> <retransmission_timeout> `

Example:
`python3 receiver.py 112 20 0.1 100` and
`java Sender image.png 112 100 120`

