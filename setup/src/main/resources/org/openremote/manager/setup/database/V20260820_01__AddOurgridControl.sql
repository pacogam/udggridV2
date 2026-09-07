-- Add attribute ourgridControl to OurgridBatteryAsset
SELECT a.id, ADD_ATTRIBUTE(a, 'ourgridControl', 'boolean', null, now(), jsonb_build_object('ruleState', true))
FROM asset a WHERE a.type = 'OurgridBatteryAsset';