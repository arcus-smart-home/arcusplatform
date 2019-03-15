A command line tool intended to allow filtering by time range across all of the pertinent topics.

Still very beta, but some sample commands:

Starting from the last 10 minutes tail the platform topic
  eye-kat -t platform --start -10m
 
Read all logs in an exact hour range:
  eye-kat -t irisLog --start "2015-12-18T14:00:00ZUS/Central" --end "2015-12-18T15:00:00ZUS/Central"
  
Read protocol messages to drivers from 3 to 4 today in the current timezone:
  eye-kat -t protocol_todrivers --start "T15:00" --end "T16:00"