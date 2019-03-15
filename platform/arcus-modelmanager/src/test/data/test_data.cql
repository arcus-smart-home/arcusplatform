USE dev;

INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-274ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Hu', 'Man', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-1309eda1122a, 6f293fd5-b65b-4a63-a5ed-1309eda1122b, 6f293fd5-b65b-4a63-a5ed-1309eda1122c}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-2510a53bcbe7);

INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (1f92c6dc-d6a2-443a-8edd-63afc61d903e, 1419012584398, 1419012632501, 'active',  true, 'Herman', 'Blount', '55 Awesome Rd.', 'AL', 'Birmingham', '35201');
  
INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (0b929bdd-64c9-4338-bcac-9ac1b3c8e54b, 1419012584398, 1419012632501, 'active',  true, 'Charles', 'Mingus', '2233 51st Street', 'NY', 'New York', '10034');
  
INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (db4d6cd0-d8ff-4ca6-847b-062974a7364b, 1419012584398, 1419012632501, 'active',  true, 'Brian', 'Ferneyhough', '158 Getaway Blvd', 'Kansas City', 'MO', '64111');
  
INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (2be97e57-9447-49c1-934b-93558d3de7db, 1419012584398, 1419012632501, 'active',  true, 'Carla', 'Kihlstedt', '255 Something St.', 'CA', 'San Francisco', '94101');

INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (72e21b60-79e9-4a4d-b099-b963f8ced088, 1419012584398, 1419012632501, 'active',  true, 'Ada', 'Lovelace', '23542 199th Street', 'KS', 'Overland Park', '66209');
  
INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (cd7a1905-8105-4b31-894e-10c99fba430a, 1419012584398, 1419012632501, 'active',  true, 'Paulina', 'Oliveros', '5 Desert Rd', 'NM', 'Santa Fe', '87501');
  
INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip) VALUES
  (ac45d03b-ad27-44a7-8a46-7745c96de1b9, 1419012584398, 1419012632501, 'active',  true, 'Sally', 'Ride', '9999 Blahlah Ave', 'MA', 'Boston', '02108');

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-2510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-274ba3605ccc, 'Rob', 'Anonymous', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'rob@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);


INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (1e1b2a51-e619-4a5a-95b1-c2300959f7b7, 1429797799471, 1429797799471, 1f92c6dc-d6a2-443a-8edd-63afc61d903e, 'Harry', 'Partch', 1429797799471, 1429797799471, 1429797799471, 1429797799471, 'partched@yahoo.com', '9135557788', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (fbd31a2e-2864-4cc3-b3d5-ac820174b47c, 1429797836636, 1429797836636, 8f29064f-97a1-4091-aef7-308917246eba, 'Herman', 'Blount', 1429797836636, 1429797836636, 1429797836636, 1429797836636, 'iamsunra@hotmail.com', '9135557654', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (c88bc1b6-8bb0-4888-b481-60d56b2b83b4, 1429797849237, 1429797849237, 0b929bdd-64c9-4338-bcac-9ac1b3c8e54b, 'Charles', 'Mingus', 1429797849237, 1429797849237, 1429797849237, 1429797849237, 'theblacksaint@gmail.com', '9135551378', { 'question3':'BgXPOmdIMIavKAsERJMVQV7ESPEdYzX1PZd+id0aJ80=','question5':'ez1egojMgT3U0Fm2a7abDH/memQTDkYJN0yLzYWioqc=','question6':'FCQK+xoZ4ItNXQp2usDdEbzdTGSenuzJvH6hlOLxOWs=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (6519dc4c-b674-43a1-a1f4-54ad44d53a4d, 1429797866221, 1429797866221, db4d6cd0-d8ff-4ca6-847b-062974a7364b, 'Brian', 'Ferneyhough', 1429797866221, 1429797866221, 1429797866221, 1429797866221, 'new.complexity@gmail.com', '9135559741', { 'question3':'EQyLGPmgoE9uL+087yLbSVyUPZ4z/iNH6ohtczqNvYuWW8gNUKm+sQ81Wi3+zLIw','question5':'DdKtPuNEzrCKu5lK9uyMP9KvrenydnFHswByOPGvzb8=','question6':'SiO3rlKzdn2Xis1T2NLifipZzXL5rDAyvq6KP3IHHHU=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (d0928ff5-1f4e-4361-bac9-a8ed69a8accb, 1429797876437, 1429797876437, c28d9dbb-544d-48c2-8643-d3e700abf7a2, 'John', 'Gilmore', 1429797876437, 1429797876437, 1429797876437, 1429797876437, 'space.is.the.place@saturninc.com', '9135554488', { 'question3':'EQyLGPmgoE9uL+087yLbSVyUPZ4z/iNH6ohtczqNvYuWW8gNUKm+sQ81Wi3+zLIw','question5':'DdKtPuNEzrCKu5lK9uyMP9KvrenydnFHswByOPGvzb8=','question6':'SiO3rlKzdn2Xis1T2NLifipZzXL5rDAyvq6KP3IHHHU=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (4e94eda2-c1ba-4f6a-a62b-841ab92932a8, 1429797888086, 1429797888086, b34a9e98-8d89-4870-914c-cc1a4193f5ad, 'Don', 'Van Vliet', 1429797888086, 1429797888086, 1429797888086, 1429797888086, 'willie@beefheart.com', '7855553322',{ 'question2':'hXwf85fQ9fPjjeo3e5XBSlB+/eYmmoRPtnVR1daheJM=','question5':'R5HFG1tIqCvNjLaA2PGwaf8hMKG+Btoe4hGrn6RLd6g=','question4':'8TG8uN/nnw6M7GiCudVKcZbXAxJfqz5PqwHunJVEVhA=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (c8d958a2-2bd4-4302-bfd5-4886c205a5c5, 1429797897102, 1429797897102, 2be97e57-9447-49c1-934b-93558d3de7db, 'Carla', 'Kihlstedt', 1429797897102, 1429797897102, 1429797897102, 1429797897102, 'kihlstedt.carla@gmail.com', '7855553328',{ 'question2':'wTPmSFnwJrJX+dtpqjLzLW/bz5sfMuF84dHx/2D03i8=','question5':'5OG5fF/2q41hFW7RgowqVrq+H3lB0/MUFmt5iKe6pys=','question4':'HwXl2j5HTGWPbVHWkBKC4hUhUMpzFAXOSSDhGXQ6cXM=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (045293c3-d35f-46b2-b1d5-548ca7fab219, 1429797919702, 1429797919702, 72e21b60-79e9-4a4d-b099-b963f8ced088, 'Ada', 'Lovelace', 1429797919702, 1429797919702, 1429797919702, 1429797919702, 'roll.with.the.punches@gmail.com', '8165550088', { 'question3':'uX1Wwgo9CdOIE8Ek16ckw/L2OdpN54tzLAXA2kFj6D8=','question6':'mVBzgT0d6Kr/F2Ao1H75PKMAuov0nguNzJVg2w/s/x8=','question1':'nFb1Z5PZDrY62ULv/AdUFctdrn8p5FtEgk4MYTekzto=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (3ed1a2bd-aa68-444d-8ab9-f2c84270963a, 1429797934862, 1429797934862, cd7a1905-8105-4b31-894e-10c99fba430a, 'Paulina', 'Oliveros', 1429797934862, 1429797934862, 1429797934862, 1429797934862, 'poliveros@oliveros.com', '7855551478',{ 'question3':'KP3goerduLY9nZN02g5s9TVreY6NpIGK9lipSUUdh04=','question6':'EYvV2kmtsLnPSWeM1lvKkev7E2XLrSvALsoEw7tHlnc=','question1':'xmVxymWxy0fwm8V9gHvLHnOm5OyYEWVi+Bb68+rG/+0=' }, 0);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (9b1a3a48-da9d-4e80-9af3-3ee817c16b52, 1429797947262, 1429797947262, ac45d03b-ad27-44a7-8a46-7745c96de1b9, 'Sally', 'Ride', 1429797947262, 1429797947262, 1429797947262, 1429797947262, 'topdog@sallyride.com', '7165552489', { 'question3':'9Yq1jKGGnwLbO5HwOy2ofIK4ucQ9W/apeJl/KHjCes8=' , 'question6':'RLs3T6G/ebRplRPabaNiG+GdV18xUieLTIjBdwXtIIo=' , 'question1':'4MBE/jf+bKwPVccOOpnXH57+21xZugFXPQ/nR7G0EdI=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'rob', 'rob@arcus.com', null, 08300b50-c109-4528-a705-2510a53bcbe7);


INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-1309eda1122a, c24b0e18-3394-4f81-b762-274ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');
INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-1309eda1122b, c24b0e18-3394-4f81-b762-274ba3605ccc, 'My Work', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'BASIC', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');
INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-1309eda1122c, c24b0e18-3394-4f81-b762-274ba3605ccc, 'My Third Place', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');
INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-1309eda1132a, 72e21b60-79e9-4a4d-b099-b963f8ced088, 'non owner place', 1419012584398, 1419012584398, 'Mooresville', 'NC', 'Walnut Street.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');


insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-2510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-1309eda1122c, c24b0e18-3394-4f81-b762-274ba3605ccc, true, {'*:*:*'}, 'My Home');
insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-2510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-1309eda1132a, 72e21b60-79e9-4a4d-b099-b963f8ced088, true, {'*:*:*'}, 'My Home');


//INSERT INTO device (id, created, modified, state, accountId, placeId, protocolName, protocolId, driverName, driverVersion, driverAddress, protocolAddress, hubId) VALUES
//  (0a875067-76de-4f1c-82f5-6e0a2f15554e, 1419012584398, 1419012632501, 'online', c24b0e18-3394-4f81-b762-274ba3605ccc, 6f293fd5-b65b-4a63-a5ed-1309eda1122a, 'Ipcd', 'BlackBox-ContactSensor-05102034A7B5', 'IpcdContactSensorDriver', '1.0', 'DRIV:dev:0a875067-76de-4f1c-82f5-6e0a2f15554e', 'PROT:IPCD:3/Z//HH/4XLLVsfu5urezCz0mMA=', null);
  
INSERT INTO device_protocolid (id, protocolName, protocolId) VALUES (0a875067-76de-4f1c-82f5-6e0a2f15554e, 'Ipcd', 'BlackBox-ContactSensor-05102034A7B5');
INSERT INTO device_protocoladdress (id, protocolAddress) VALUES (0a875067-76de-4f1c-82f5-6e0a2f15554e, 'PROT:IPCD:3/Z//HH/4XLLVsfu5urezCz0mMA=');

//INSERT INTO device (id, created, modified, state, accountId, protocolName, protocolId, driverName, driverVersion, driverAddress, protocolAddress, hubId) VALUES
//  (4f2397e1-1f0f-45ba-abdc-4c1e829fa538, 1419012584398, 1419012632501, 'online', c24b0e18-3394-4f81-b762-274ba3605ccc, 'Ipcd', 'BlackBox-Switch1-123456789', 'IpcdSwitchDriver', '1.0', 'DRIV:dev:4f2397e1-1f0f-45ba-abdc-4c1e829fa538', 'PROT:IPCD:qvIT3SYt1xQKbMPX6qzeHdNJA3U=', null);
  
INSERT INTO device_protocolid (id, protocolName, protocolId) VALUES (4f2397e1-1f0f-45ba-abdc-4c1e829fa538, 'Ipcd', 'BlackBox-Switch1-123456789');
INSERT INTO device_protocoladdress (id, protocolAddress) VALUES (4f2397e1-1f0f-45ba-abdc-4c1e829fa538, 'PROT:IPCD:qvIT3SYt1xQKbMPX6qzeHdNJA3U=');

INSERT INTO hub (id, created, modified, accountId, placeId) VALUES ('ABC-2345', 1419012584398, 1419012584398, c24b0e18-3394-4f81-b762-274ba3605ccc, 6f293fd5-b65b-4a63-a5ed-1309eda1122a);

//INSERT INTO device (id, created, modified, state, accountId, protocolName, protocolId, driverName, driverVersion, driverAddress, protocolAddress, hubId) VALUES
//  (6ff3a5ff-4650-4ce7-82e0-682a58392316, 1419012584398, 1419012632501, 'active', c24b0e18-3394-4f81-b762-274ba3605ccc, 'ZWave', '34efd213-22', 'ZWaveGenericSwitchDriver', '1.0', 'DRIV:dev:6ff3a5ff-4650-4ce7-82e0-682a58392316', 'PROT:ZWAV-93de863d-d6d9-446b-939b-97f845662139:MjIAAAAAAAAAAAAAAAAAAAAAAAA=', 'ABC-1234');

INSERT INTO device_protocolid (id, protocolName, protocolId) VALUES (6ff3a5ff-4650-4ce7-82e0-682a58392316, 'ZWave', '34efd213-22');
INSERT INTO device_protocoladdress (id, protocolAddress) VALUES (6ff3a5ff-4650-4ce7-82e0-682a58392316, 'PROT:ZWAV-93de863d-d6d9-446b-939b-97f845662139:MjIAAAAAAAAAAAAAAAAAAAAAAAA=');

//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question1', 'Mother''s Maiden Name?');
//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question2', 'Father''s Middle Name?');
//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question3', 'First Car?');
//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question4', 'Favorite Pet?');
//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question5', 'Favorite Teacher?');
//INSERT INTO resource_bundle (bundle, locale, key, value) VALUES ('security_question', 'en', 'question6', 'High School Mascot?');





INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-374ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Rob', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-3309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-3510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-3510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-374ba3605ccc, 'Rob', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'robert.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'rob', 'robert.l.smith@arcus.com', null, 08300b50-c109-4528-a705-3510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-3309eda1122a, c24b0e18-3394-4f81-b762-374ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-3510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-3309eda1122a, c24b0e18-3394-4f81-b762-374ba3605ccc, true, {'*:*:*'}, 'My Home');


INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-474ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Bob', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-4309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-4510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-4510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-474ba3605ccc, 'Bob', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'bob.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'bob', 'bob.l.smith@arcus.com', null, 08300b50-c109-4528-a705-4510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-4309eda1122a, c24b0e18-3394-4f81-b762-474ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-4510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-4309eda1122a, c24b0e18-3394-4f81-b762-474ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-574ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Bill', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-5309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-5510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-5510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-574ba3605ccc, 'Bill', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'bill.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'bil', 'bill.l.smith@arcus.com', null, 08300b50-c109-4528-a705-5510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-5309eda1122a, c24b0e18-3394-4f81-b762-574ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-5510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-5309eda1122a, c24b0e18-3394-4f81-b762-574ba3605ccc, true, {'*:*:*'}, 'My Home');




INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-674ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Steve', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-6309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-6510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-6510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-674ba3605ccc, 'Steve', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'steve.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'ste', 'steve.l.smith@arcus.com', null, 08300b50-c109-4528-a705-6510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-6309eda1122a, c24b0e18-3394-4f81-b762-674ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-6510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-6309eda1122a, c24b0e18-3394-4f81-b762-674ba3605ccc, true, {'*:*:*'}, 'My Home');




INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-774ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Tim', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-7309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-7510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-7510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-774ba3605ccc, 'Tim', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'tim.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'tim', 'tim.l.smith@arcus.com', null, 08300b50-c109-4528-a705-7510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-7309eda1122a, c24b0e18-3394-4f81-b762-774ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-7510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-7309eda1122a, c24b0e18-3394-4f81-b762-774ba3605ccc, true, {'*:*:*'}, 'My Home');




INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-874ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Fred', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-8309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-8510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-8510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-874ba3605ccc, 'Fred', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'fred.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'fre', 'fred.l.smith@arcus.com', null, 08300b50-c109-4528-a705-8510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-8309eda1122a, c24b0e18-3394-4f81-b762-874ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-8510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-8309eda1122a, c24b0e18-3394-4f81-b762-874ba3605ccc, true, {'*:*:*'}, 'My Home');




INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-974ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Tom', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-9309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-9510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-9510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-974ba3605ccc, 'Tom', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'tom.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'tom', 'tom.l.smith@arcus.com', null, 08300b50-c109-4528-a705-9510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-9309eda1122a, c24b0e18-3394-4f81-b762-974ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-9510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-9309eda1122a, c24b0e18-3394-4f81-b762-974ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-074ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Wes', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-0309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-0510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-0510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-074ba3605ccc, 'Wes', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'wes.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'wes', 'wes.l.smith@arcus.com', null, 08300b50-c109-4528-a705-0510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-0309eda1122a, c24b0e18-3394-4f81-b762-074ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-0510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-0309eda1122a, c24b0e18-3394-4f81-b762-074ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-a74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Brian', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-a309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-a510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-a510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-a74ba3605ccc, 'Brian', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'brian.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'bri', 'brian.l.smith@arcus.com', null, 08300b50-c109-4528-a705-a510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-a309eda1122a, c24b0e18-3394-4f81-b762-a74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-a510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-a309eda1122a, c24b0e18-3394-4f81-b762-a74ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-b74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Cody', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-b309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-b510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-b510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-b74ba3605ccc, 'Cody', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'cody.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'cod', 'cody.l.smith@arcus.com', null, 08300b50-c109-4528-a705-b510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-b309eda1122a, c24b0e18-3394-4f81-b762-b74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-b510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-b309eda1122a, c24b0e18-3394-4f81-b762-b74ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-c74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Brandon', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-c309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-c510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-c510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-c74ba3605ccc, 'Brandon', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'brandon.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'bra', 'brandon.l.smith@arcus.com', null, 08300b50-c109-4528-a705-c510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-c309eda1122a, c24b0e18-3394-4f81-b762-c74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-c510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-c309eda1122a, c24b0e18-3394-4f81-b762-c74ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-d74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Emily', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-d309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-d510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-d510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-d74ba3605ccc, 'Emily', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'emily.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'emi', 'emily.l.smith@arcus.com', null, 08300b50-c109-4528-a705-d510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-d309eda1122a, c24b0e18-3394-4f81-b762-d74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-d510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-d309eda1122a, c24b0e18-3394-4f81-b762-d74ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-e74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Sophia', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-e309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-e510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-e510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-e74ba3605ccc, 'Sophia', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'sophia.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'sop', 'sophia.l.smith@arcus.com', null, 08300b50-c109-4528-a705-e510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-e309eda1122a, c24b0e18-3394-4f81-b762-e74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-e510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-e309eda1122a, c24b0e18-3394-4f81-b762-e74ba3605ccc, true, {'*:*:*'}, 'My Home');



INSERT INTO account (id, created, modified, state,  billable, billingfirstname, billinglastname, billingstreet1, billingstate, billingcity, billingzip, placeids, subscriptionids, owner) VALUES
  (c24b0e18-3394-4f81-b762-f74ba3605ccc, 1419012584398, 1419012632501, 'active',  true, 'Deanna', 'Smith', '123 Fake St', 'IL', 'Springfield', '62701', {6f293fd5-b65b-4a63-a5ed-f309eda1122a}, {'PREMIUM':'2e981583e438911cbb9b46498a9b1e46', 'BASIC': '2e9971c38aaf81e26840b3458d878878'}, 08300b50-c109-4528-a705-f510a53bcbe7);

INSERT INTO person (id, created, modified, accountId, firstName, lastName, termsAgreed, privacyPolicyAgreed, consentOffersPromotions, consentStatement, email, mobileNumber, securityAnswers, mobileDeviceSequence) VALUES
  (08300b50-c109-4528-a705-f510a53bcbe7, 1420741425043, 1420741425043, c24b0e18-3394-4f81-b762-f74ba3605ccc, 'Deanna', 'Smith', 1420741425043, 1420741425043, 1420741425043, 1429797799471,'deanna.l.smith@arcus.com', '5555555555', { 'question3':'AluWLJUQeW5gxSQTE2OzLaANLpQDhYWiCPzNX9m74l0=','question5':'tHGVslMBIXcUYAqlkf+1HPBdYbpBttf7humS75eakgk=','question4':'nZdFZYBtfVUOfYTXjDs9i0xVu48ux3YWNvF4hbD1c2M=' }, 0);

INSERT INTO login (domain, user_0_3, user, password, personId) VALUES ('arcus.com', 'dea', 'deanna.l.smith@arcus.com', null, 08300b50-c109-4528-a705-f510a53bcbe7);

INSERT INTO place (id, accountId, name, created, modified, city, state, streetaddress1, streetaddress2, zipcode, servicelevel, populationid, tzname) VALUES (6f293fd5-b65b-4a63-a5ed-f309eda1122a, c24b0e18-3394-4f81-b762-f74ba3605ccc, 'My Home', 1419012584398, 1419012584398, 'Mooresville', 'NC', '123 Street St.' , 'Apt. C', '90210', 'PREMIUM', 00000000-0000-0000-0000-000000000000, 'America/Phoenix');

insert into authorization_grant (entityid, placeid, accountid, accountowner, permissions, placename)values (08300b50-c109-4528-a705-f510a53bcbe7, 6f293fd5-b65b-4a63-a5ed-f309eda1122a, c24b0e18-3394-4f81-b762-f74ba3605ccc, true, {'*:*:*'}, 'My Home');

