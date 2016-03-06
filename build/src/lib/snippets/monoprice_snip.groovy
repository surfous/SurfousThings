// ORIGIN REGION BEGIN disambiguation
// for fingerprint disambiguation
@Field final Map DEVICE_PRODUCT_ID_DISAMBIGUATION = [
	'0x2001': 'Monoprice NO-LABEL Door & Window Sensor (Contact Sensor)',
	'0x2003': 'Monoprice NO-LABEL Shock Sensor (Acceleration Sensor)',
	'0x200a': 'Monoprice NO-LABEL Garage Door Sensor (Contact Sensor via tilt)'
	].withDefault {UNKNOWN.NAME}
// ORIGIN REGION END disambiguation
