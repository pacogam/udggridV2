-- Add attribute estimatedSolarCapacity to OurGrid meter Asset
SELECT a.id, ADD_ATTRIBUTE(a, 'estimatedSolarCapacity', 'number', null, now(), jsonb_build_object('accessRestrictedRead', true, 'readOnly', true, 'ruleState', true, 'storeDataPoints', true))
FROM asset a WHERE a.type = 'OurgridMeterAsset';

-- Add attribute estimateSolarCapacityManually to OurGrid meter Asset
SELECT a.id, ADD_ATTRIBUTE(a, 'estimateSolarCapacityManually', 'boolean', null, now(), jsonb_build_object('accessRestrictedRead', true, 'ruleState', true))
FROM asset a WHERE a.type = 'OurgridMeterAsset';