SELECT
mri.`mrid`,
mri.`upc`,
mri.lastDateScanned,
	mri.qtyScanned,
	productId,
	uomFromid,
	product.num,
	product.`partId` ,
	bi_upctouomFull.`multiply`,
	bi_upctouomFull.`multiply` * mri.qtyScanned AS totalReceived

FROM (SELECT bi_mri.`mrid`, bi_mri.`upc`, MAX(bi_mri.`timeScanned`) AS lastDateScanned, COUNT(bi_mri.`upc`) AS qtyScanned
	FROM bi_mri
	GROUP BY bi_mri.`upc`, mrid) mri

	LEFT JOIN (SELECT bi_upctouom.`upcCode`, bi_upctouom.`productId`, uomconversion.`multiply`, uom.`name` AS uomname, bi_upctouom.`uomFromId`
		FROM bi_upctouom
		JOIN uomconversion ON uomconversion.`fromUomId` = bi_upctouom.`uomFromId` AND uomconversion.`toUomId`= 1
		JOIN uom ON uom.`id` = uomconversion.`fromUomId`

		UNION

		SELECT product.`upc`, product.id AS productid, 1 AS multiply, 'Each' AS UomName, product.`uomId`
		FROM product
		WHERE product.`uomId` = 1 AND product.id IN (SELECT productId FROM bi_upctouom)) bi_upctouomFull ON bi_upctouomFull.`upcCode` = mri.`upc`




JOIN product ON bi_upctouomFull.productId = product.`id`

WHERE mri.mrid = %1$s