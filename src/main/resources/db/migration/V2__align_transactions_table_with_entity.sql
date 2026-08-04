/*!80000 SET @add_payee_name = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'payee_name'
		),
		'SELECT 1',
		'ALTER TABLE transactions ADD COLUMN payee_name VARCHAR(128) NULL'
	)
) */;
/*!80000 PREPARE stmt FROM @add_payee_name */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;

/*!80000 SET @add_location = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'location'
		),
		'SELECT 1',
		'ALTER TABLE transactions ADD COLUMN location VARCHAR(255) NULL'
	)
) */;
/*!80000 PREPARE stmt FROM @add_location */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;

/*!80000 SET @add_latitude = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'latitude'
		),
		'SELECT 1',
		'ALTER TABLE transactions ADD COLUMN latitude DECIMAL(10,7) NULL'
	)
) */;
/*!80000 PREPARE stmt FROM @add_latitude */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;

/*!80000 SET @add_longitude = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'longitude'
		),
		'SELECT 1',
		'ALTER TABLE transactions ADD COLUMN longitude DECIMAL(10,7) NULL'
	)
) */;
/*!80000 PREPARE stmt FROM @add_longitude */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;

/*!80000 SET @fix_latitude = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'latitude'
			  AND (data_type <> 'decimal' OR numeric_precision <> 10 OR numeric_scale <> 7)
		),
		'ALTER TABLE transactions MODIFY COLUMN latitude DECIMAL(10,7) NULL',
		'SELECT 1'
	)
) */;
/*!80000 PREPARE stmt FROM @fix_latitude */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;

/*!80000 SET @fix_longitude = (
	SELECT IF(
		EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = DATABASE()
			  AND table_name = 'transactions'
			  AND column_name = 'longitude'
			  AND (data_type <> 'decimal' OR numeric_precision <> 10 OR numeric_scale <> 7)
		),
		'ALTER TABLE transactions MODIFY COLUMN longitude DECIMAL(10,7) NULL',
		'SELECT 1'
	)
) */;
/*!80000 PREPARE stmt FROM @fix_longitude */;
/*!80000 EXECUTE stmt */;
/*!80000 DEALLOCATE PREPARE stmt */;


