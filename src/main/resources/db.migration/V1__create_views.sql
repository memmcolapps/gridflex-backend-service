DROP VIEW IF EXISTS public.vw_event_details;
--1. public.vw_meter_consumption
CREATE OR REPLACE VIEW public.vw_meter_consumption
 AS
SELECT vt.id AS meter_id,
       vt.meter_number,
       vt.account_number,
       vt.org_id,
       vt.node_id,
       vt.dss,
       vt.meter_class,
       vt.meter_category,
       vt.type,
       vt.fixed_energy,
       t.name,
       t.tariff_type,
       concat(c.firstname, ' ', c.lastname) AS customer_name,
       concat(lo.house_no, ',', lo.street_name, ',', lo.city, ',', lo.state) AS meter_address,
       s.name AS dss_name,
       mc.consumption_type,
       COALESCE(mc.current_reading, 0::numeric) AS current_reading,
       COALESCE(mc.previous_reading, 0::numeric) AS previous_reading,
       COALESCE(mc.cumulative_reading, 0::numeric) AS cumulative_reading,
       COALESCE(mc.average_consumption, 0::numeric) AS average_consumption,
       COALESCE(mc.consumption, 0::numeric) AS consumption,
       COALESCE(mc.prev_consumption, 0::numeric) AS prev_consumption,
       mc.reading_date
FROM meters vt
         LEFT JOIN tariffs t ON vt.tariff = t.id
         LEFT JOIN meter_consumption mc ON mc.meter_id = vt.id
         LEFT JOIN customers c ON c.customer_id::text = vt.customer_id::text
     LEFT JOIN meter_assign_locations lo ON vt.id = lo.meter_id
    LEFT JOIN substation_trans_feeder_lines s ON vt.dss = s.node_id
WHERE vt.meter_stage::text = 'Assigned'::text AND vt.status::text = 'Active'::text;


--2. public.vw_meter_non_md_consumption
CREATE OR REPLACE VIEW public.vw_meter_non_md_consumption
 AS
SELECT s.org_id,
       s.node_id,
       t.id AS tariff_id,
       t.name AS tariff_name,
       t.tariff_type,
       count(vt.id) AS meter_count,
       COALESCE(mc.previous_consumption, 0::numeric) AS previous_consumption,
       COALESCE(mc.consumption_per_meter, 0::numeric) AS consumption_per_meter,
       mc.reading_date,
       mc.created_at
FROM substation_trans_feeder_lines s
         LEFT JOIN meters vt ON vt.node_id = s.node_id AND vt.meter_stage::text = 'Assigned'::text AND vt.status::text = 'Active'::text AND vt.meter_class::text = 'Non-MD'::text AND vt.type::text = 'VIRTUAL'::text AND vt.fixed_energy IS NULL
     LEFT JOIN tariffs t ON vt.tariff = t.id
    LEFT JOIN meter_non_md_consumption mc ON mc.node_id = s.node_id
WHERE s.type::text = 'feeder line'::text
GROUP BY s.org_id, s.node_id, t.id, t.name, t.tariff_type, mc.previous_consumption, mc.consumption_per_meter, mc.reading_date, mc.created_at;


--3. public.vw_vending_transactions_summary
CREATE OR REPLACE VIEW public.vw_vending_transactions_summary
 AS
SELECT vt.id AS transaction_id,
       vt.org_id,
       vt.customer_id,
       vt.user_id,
       m.account_number AS meter_account_number,
       m.id AS meter_id,
       m.node_id,
       m.service_center,
       m.region,
       m.meter_number,
       m.meter_category,
       m.meter_class,
       vt.initial_amount,
       vt.final_amount,
       vt.vat_amount,
       vt.unit,
       vt.unit_cost,
       vt.status,
       vt.token,
       vt.kct1,
       vt.kct2,
       vt.receipt_no,
       vt.tx_node_id,
       vt.token_type,
       t.name AS tariff_name,
       t.tariff_rate,
       b.name AS band_name,
       b.hour AS band_hour,
       concat(u.firstname, ' ', u.lastname) AS user_fullname,
       concat(c.firstname, ' ', c.lastname) AS customer_fullname,
       concat(lo.house_no, ',', lo.street_name, ',', lo.city, ',', lo.state) AS address,
       vt.created_at,
       vt.updated_at
FROM vending_transactions vt
         LEFT JOIN meters m ON vt.meter_id = m.id
         LEFT JOIN customers c ON m.customer_id::text = c.customer_id::text
     LEFT JOIN meter_assign_locations lo ON vt.meter_id = lo.meter_id
    LEFT JOIN tariffs t ON m.tariff = t.id
    LEFT JOIN bands b ON t.band_id = b.id
    LEFT JOIN users u ON vt.user_id = u.id;

--4. public.vw_meter_summary
CREATE OR REPLACE VIEW public.vw_meter_summary
 AS
SELECT vt.id AS meter_id,
       vt.org_id,
       vt.customer_id,
       vt.account_number AS meter_account_number,
       vt.meter_number,
       vt.cin AS meter_cin,
       vt.sim_number,
       vt.meter_class,
       vt.meter_category,
       vt.meter_stage,
       vt.status,
       vt.region,
       vt.node_id,
       n.name AS bhub_name,
       vt.service_center,
       vt.substation,
       vt.feeder,
       vt.dss,
       c.vat,
       vt.type,
       vt.old_sgc,
       vt.new_sgc,
       vt.old_krn,
       vt.new_krn,
       vt.old_tariff_index,
       vt.new_tariff_index,
       t.id AS tariff_id,
       t.name AS tariff_name,
       t.tariff_rate,
       b.id AS band_id,
       b.name AS band_name,
       b.hour AS band_hour,
       ev.connection_type,
       ev.online_time,
       ev.offline_time,
       md.ct_ratio_num AS md_ct_ratio_num,
       md.ct_ratio_denom AS md_ct_ratio_denom,
       md.volt_ratio_num AS md_volt_ratio_num,
       md.volt_ratio_denom AS md_volt_ratio_denom,
       md.multiplier AS md_multiplier,
       md.meter_rating AS md_meter_ration,
       md.initial_reading AS md_initial_reading,
       md.dial AS md_dial,
       ms.meter_model AS smart_meter_model,
       ms.protocol AS smart_meter_protocol,
       cda.id AS adjustment_id,
       cda.debit AS debit_amount,
       cda.balance AS balance_after_adjustment,
       cda.type AS adjustment_type,
       cda.status AS adjustment_status,
       COALESCE(sum(cdp.credit), 0::numeric) AS credit_amount,
       pm.debit_payment_mode,
       pm.debit_payment_plan,
       pm.credit_payment_mode,
       pm.credit_payment_plan,
       lc.name AS liability_name,
       lc.code AS liability_code,
       concat(c.firstname, ' ', c.lastname) AS customer_fullname,
       concat(lo.house_no, ',', lo.street_name, ',', lo.city, ',', lo.state) AS address,
       mr.name AS manufacturer_name,
       vt.created_at,
       vt.updated_at,
       cda.created_at AS cda_created_at,
       cda.updated_at AS cda_updated_at
FROM meters vt
         LEFT JOIN customers c ON vt.customer_id::text = c.customer_id::text
     LEFT JOIN meter_assign_locations lo ON vt.id = lo.meter_id
    LEFT JOIN payment_mode pm ON pm.meter_id = vt.id AND pm.status = true
    LEFT JOIN md_meters_info md ON vt.id = md.meter_id
    LEFT JOIN smart_meter_info ms ON vt.id = ms.meter_id
    LEFT JOIN manufacturers mr ON vt.meter_manufacturer = mr.id
    LEFT JOIN tariffs t ON vt.tariff = t.id
    LEFT JOIN bands b ON t.band_id = b.id
    LEFT JOIN credit_debit_adjustment cda ON cda.meter_id = vt.id
    LEFT JOIN credit_debit_payment cdp ON cdp.credit_debit_adj_id = cda.id
    LEFT JOIN liability_cause lc ON lc.id = cda.liability_cause_id
    LEFT JOIN vw_node_summary n ON n.node_id = vt.node_id
    LEFT JOIN meters_connection_event ev ON ev.meter_no::text = vt.meter_number::text
GROUP BY vt.id, vt.org_id, vt.customer_id, vt.account_number, vt.meter_number, vt.cin, vt.meter_class, vt.meter_category, vt.meter_stage, vt.status, vt.region, vt.sim_number, vt.node_id, vt.service_center, vt.substation, n.name, c.vat, vt.type, vt.old_sgc, vt.new_sgc, vt.old_krn, vt.new_krn, vt.old_tariff_index, vt.new_tariff_index, t.id, t.name, t.tariff_rate, b.id, b.name, b.hour, md.ct_ratio_num, md.ct_ratio_denom, md.volt_ratio_num, md.volt_ratio_denom, md.multiplier, md.meter_rating, md.initial_reading, md.dial, ms.meter_model, ms.protocol, cda.id, cda.debit, cda.balance, cda.type, cda.status, pm.debit_payment_mode, pm.debit_payment_plan, pm.credit_payment_mode, pm.credit_payment_plan, lc.name, lc.code, c.firstname, c.lastname, lo.house_no, lo.street_name, lo.city, lo.state, mr.name, vt.created_at, vt.updated_at, cda.created_at, cda.updated_at, ev.connection_type, ev.online_time, ev.offline_time;


--5. public.vw_overall_feeder_consumption

CREATE OR REPLACE VIEW public.vw_overall_feeder_consumption
 AS
SELECT vt.node_id,
       vt.asset_id,
       vt.org_id,
       vt.name AS feeder_name,
       COALESCE(sum(c.feeder_consumption), 0::numeric) AS total_feeder_consumption,
       COALESCE(sum(v.consumption), 0::numeric) AS total_postpaid_consumption,
       COALESCE(sum(t.final_amount), 0::numeric) AS total_prepaid_consumption,
       COALESCE(sum(vc.consumption), 0::numeric) AS total_md_virtual_consumption,
       COALESCE(sum(vn.consumption), 0::numeric) AS total_non_md_virtual_consumption,
       COALESCE(c.technical_loss, 0::numeric) AS technical_loss,
       COALESCE(c.commercial_loss, 0::numeric) AS commercial_loss,
       c.billing_date,
       COALESCE(
               CASE
                   WHEN sum(c.feeder_consumption) = 0::numeric THEN 0::numeric
               ELSE (COALESCE(sum(v.consumption), 0::numeric) + COALESCE(sum(t.final_amount), 0::numeric) + COALESCE(sum(vc.consumption), 0::numeric) + COALESCE(sum(vn.consumption), 0::numeric)) / sum(c.feeder_consumption) * 100::numeric
               END, 0::numeric) AS efficiency_score,
       COALESCE(sum(c.feeder_consumption), 0::numeric) - (COALESCE(sum(v.consumption), 0::numeric) + COALESCE(sum(t.final_amount), 0::numeric) + COALESCE(sum(vc.consumption), 0::numeric) + COALESCE(sum(vn.consumption), 0::numeric)) AS total_energy_left,
       count(DISTINCT
             CASE
                 WHEN m.meter_category::text = 'Prepaid'::text THEN m.id
                 ELSE NULL::uuid
                 END) AS prepaid_meter_count,
       count(DISTINCT
             CASE
                 WHEN m.meter_category::text = 'Postpaid'::text AND m.meter_class::text = 'MD'::text AND m.type::text = 'VIRTUAL'::text THEN m.id
             ELSE NULL::uuid
             END) AS md_postpaid_meter_count,
       count(DISTINCT
             CASE
                 WHEN m.meter_category::text = 'Postpaid'::text AND (m.meter_class::text = ANY (ARRAY['Non-MD'::character varying, 'Single-Phase'::character varying, 'Three-Phase'::character varying]::text[])) AND m.type::text = 'VIRTUAL'::text THEN m.id
             ELSE NULL::uuid
             END) AS non_md_postpaid_meter_count
FROM substation_trans_feeder_lines vt
         LEFT JOIN feeder_consumption c ON vt.node_id = c.node_id AND vt.org_id = c.org_id
         LEFT JOIN vw_meter_consumption v ON vt.node_id = v.node_id AND vt.org_id = v.org_id AND v.meter_category::text = 'Postpaid'::text AND v.fixed_energy IS NOT NULL AND v.type::text = 'VIRTUAL'::text
     LEFT JOIN vw_vending_transactions_summary t ON vt.node_id = t.node_id AND vt.org_id = t.org_id
    LEFT JOIN vw_meter_consumption vc ON vt.node_id = vc.node_id AND vt.org_id = vc.org_id AND vc.meter_class::text = 'MD'::text AND vc.type::text = 'VIRTUAL'::text AND vc.meter_category::text = 'Postpaid'::text AND vc.fixed_energy IS NULL
    LEFT JOIN vw_meter_consumption vn ON vt.node_id = vn.node_id AND vt.org_id = vn.org_id AND vn.meter_class::text = 'Non-MD'::text AND vn.type::text = 'VIRTUAL'::text AND vn.meter_category::text = 'Postpaid'::text AND vn.fixed_energy IS NULL
    LEFT JOIN meters m ON m.node_id = vt.node_id AND m.org_id = vt.org_id AND m.status::text = 'Active'::text AND m.meter_stage::text = 'Assigned'::text
WHERE vt.type::text = 'feeder line'::text
GROUP BY vt.asset_id, vt.node_id, vt.org_id, vt.name, c.technical_loss, c.commercial_loss, c.billing_date;


--6. public.vw_meter_event_summary
CREATE OR REPLACE VIEW public.vw_meter_event_summary
 AS
SELECT el.id AS event_id,
       el.meter_serial,
       el.event_name,
       el.event_time,
       el.event_code,
       el.current_threshold,
       el.created_at,
       et.id AS event_type_id,
       et.name AS event_type_name,
       et.description AS event_type_desc,
       et.obis_code,
       vt.id AS meter_id,
       vt.org_id,
       vt.cin AS meter_cin,
       vt.smart_status,
       vt.meter_category,
       s.meter_model,
       vt.node_id,
       vt.dss,
       t.name AS tariff_name,
       t.tariff_rate,
       b.name AS band_name,
       b.hour AS band_hour,
       concat(c.firstname, ' ', c.lastname) AS customer_fullname,
       concat(lo.house_no, ',', lo.street_name, ',', lo.city, ',', lo.state) AS address
FROM event_log el
         LEFT JOIN event_type et ON et.id = el.event_type_id
         LEFT JOIN meters vt ON vt.meter_number::text = el.meter_serial::text
     LEFT JOIN smart_meter_info s ON vt.id = s.meter_id
    LEFT JOIN customers c ON vt.customer_id::text = c.customer_id::text
    LEFT JOIN meter_assign_locations lo ON vt.id = lo.meter_id
    LEFT JOIN manufacturers mr ON vt.meter_manufacturer = mr.id
    LEFT JOIN tariffs t ON vt.tariff = t.id
    LEFT JOIN bands b ON t.band_id = b.id;

--7. public.vw_node_summary
CREATE OR REPLACE VIEW public.vw_node_summary
 AS
SELECT region_bhub_service_centers.id,
       region_bhub_service_centers.region_id,
       region_bhub_service_centers.org_id,
       region_bhub_service_centers.node_id,
       region_bhub_service_centers.name,
       NULL::character varying AS serial_no,
    region_bhub_service_centers.phone_number,
    region_bhub_service_centers.email,
    region_bhub_service_centers.contact_person,
    region_bhub_service_centers.address,
    NULL::boolean AS status,
    NULL::character varying AS voltage,
    NULL::character varying AS latitude,
    NULL::character varying AS longitude,
    NULL::text AS description,
    region_bhub_service_centers.created_at,
    region_bhub_service_centers.updated_at,
    region_bhub_service_centers.type,
    NULL::character varying AS asset_id,
    region_bhub_service_centers.parent_id
FROM region_bhub_service_centers
UNION
SELECT substation_trans_feeder_lines.id,
       NULL::character varying AS region_id,
    substation_trans_feeder_lines.org_id,
    substation_trans_feeder_lines.node_id,
    substation_trans_feeder_lines.name,
    substation_trans_feeder_lines.serial_no,
    substation_trans_feeder_lines.phone_number,
    substation_trans_feeder_lines.email,
    substation_trans_feeder_lines.contact_person,
    substation_trans_feeder_lines.address,
    substation_trans_feeder_lines.status,
    substation_trans_feeder_lines.voltage,
    substation_trans_feeder_lines.latitude,
    substation_trans_feeder_lines.longitude,
    substation_trans_feeder_lines.description,
    substation_trans_feeder_lines.created_at,
    substation_trans_feeder_lines.updated_at,
    substation_trans_feeder_lines.type,
    substation_trans_feeder_lines.asset_id,
    substation_trans_feeder_lines.parent_id
FROM substation_trans_feeder_lines;


--8. public.vw_meter_obis_mapping

CREATE OR REPLACE VIEW public.vw_meter_obis_mapping
 AS
SELECT m.meter_number,
       smi.meter_model,
       m.meter_stage,
       m.meter_class,
       omd.description,
       omd.class_id,
       omd.obis_code,
       omd.attribute_index,
       omd.data_type,
       omd.unit,
       omd.meter_type,
       omd.obis_code_combined,
       omd.group_name,
       omd.data_index,
       omd.obis_type,
       omd.operation_code
FROM meters m
         LEFT JOIN smart_meter_info smi ON smi.meter_id = m.id
         LEFT JOIN obis_mapping_data omd ON upper(m.meter_class::text) = upper('md'::text) AND upper(omd.obis_type::text) = upper('md'::text) OR (upper(m.meter_class::text) = ANY (ARRAY['SINGLE-PHASE'::text, 'THREE-PHASE'::text])) AND upper(omd.obis_type::text) = upper('non-md'::text)
WHERE m.smart_status = true AND (m.meter_stage::text = ANY (ARRAY['Assign-edited'::character varying, 'Assigned'::character varying, 'Unassigned'::character varying]::text[]));

--9. public.vw_event_details
CREATE OR REPLACE VIEW public.vw_event_details
 AS
SELECT u.src_table,
       u.id,
       u.meter_no,
       u.meter_model,
       u.event_time,
       u.event_type_id,
       et.name AS event_type,
       ecl.event_name AS event,
       u.recharge_token,
       u.recharge_amount_kwh,
       u.manage_token,
       u.manage_token_type_code,
       u.mgt_token_type_description,
       u.reason_description,
       u.reason_of_operation_code,
       u.total_absolute_active_kwh,
       u.balance_kwh,
       COALESCE(ecl.critical_level, 1) AS critical_level
FROM ( SELECT 'EVENT_LOG'::text AS src_table,
               event_log.id,
              event_log.meter_serial::text AS meter_no,
               event_log.meter_model::text AS meter_model,
               event_log.event_time,
              event_log.event_type_id,
              event_log.event_code::text AS event_code,
               NULL::text AS recharge_token,
               NULL::numeric AS recharge_amount_kwh,
               NULL::text AS manage_token,
               NULL::text AS manage_token_type_code,
               NULL::text AS mgt_token_type_description,
               NULL::text AS reason_description,
               NULL::text AS reason_of_operation_code,
               NULL::numeric AS total_absolute_active_kwh,
               NULL::numeric AS balance_kwh
       FROM event_log
       UNION ALL
       SELECT 'RECHARGE'::text AS src_table,
               household_recharge_token_event.id,
              household_recharge_token_event.meter_serial::text AS meter_serial,
               household_recharge_token_event.meter_model::text AS meter_model,
               household_recharge_token_event.event_time,
              household_recharge_token_event.event_type_id,
              household_recharge_token_event.event_code::text AS event_code,
               household_recharge_token_event.recharge_token::text AS recharge_token,
               household_recharge_token_event.recharge_amount_kwh::numeric AS recharge_amount_kwh,
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               NULL::numeric AS "numeric",
               NULL::numeric AS "numeric"
       FROM household_recharge_token_event
       UNION ALL
       SELECT 'MANAGEMENT'::text AS src_table,
               household_management_token_event.id,
              household_management_token_event.meter_serial::text AS meter_serial,
               household_management_token_event.meter_model::text AS meter_model,
               household_management_token_event.event_time,
              household_management_token_event.event_type_id,
              household_management_token_event.event_code::text AS event_code,
               NULL::text AS text,
               NULL::numeric AS "numeric",
               household_management_token_event.manage_token::text AS manage_token,
               household_management_token_event.manage_token_type_code::text AS manage_token_type_code,
               household_management_token_event.mgt_token_type_description::text AS mgt_token_type_description,
               NULL::text AS text,
               NULL::text AS text,
               NULL::numeric AS "numeric",
               NULL::numeric AS "numeric"
       FROM household_management_token_event
       UNION ALL
       SELECT 'CONTROL'::text AS src_table,
               household_control_event.id,
              household_control_event.meter_serial::text AS meter_serial,
               household_control_event.meter_model::text AS meter_model,
               household_control_event.event_time,
              household_control_event.event_type_id,
              household_control_event.event_code::text AS event_code,
               NULL::text AS text,
               NULL::numeric AS "numeric",
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               household_control_event.reason_description::text AS reason_description,
               household_control_event.reason_of_operation_code::text AS reason_of_operation_code,
               NULL::numeric AS "numeric",
               NULL::numeric AS "numeric"
       FROM household_control_event
       UNION ALL
       SELECT 'FRAUD'::text AS src_table,
               household_fraud_event.id,
              household_fraud_event.meter_serial::text AS meter_serial,
               household_fraud_event.meter_model::text AS meter_model,
               household_fraud_event.event_time,
              household_fraud_event.event_type_id,
              household_fraud_event.event_code::text AS event_code,
               NULL::text AS text,
               NULL::numeric AS "numeric",
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               NULL::text AS text,
               household_fraud_event.total_absolute_active_kwh::numeric AS total_absolute_active_kwh,
               household_fraud_event.balance_kwh::numeric AS balance_kwh
       FROM household_fraud_event) u
         LEFT JOIN event_type et ON et.id = u.event_type_id
         LEFT JOIN event_code_lookup ecl ON ecl.event_type_id = u.event_type_id
                    AND ecl.code::text = u.event_code
                    AND ecl.meter_model::text ~~* u.meter_model;

-- CREATE OR REPLACE VIEW public.vw_event_details
--  AS
-- SELECT u.src_table,
--        u.id,
--        u.meter_no,
--        u.meter_model,
--        u.event_time,
--        u.event_type_id,
--        et.name AS event_type,
--        ecl.event_name AS event,
--        u.recharge_token,
--        u.recharge_amount_kwh,
--        u.manage_token,
--        u.manage_token_type,
--        COALESCE(ecl.critical_level, 1) AS critical_level
-- FROM ( SELECT 'EVENT_LOG'::text AS src_table,
--                event_log.id,
--               event_log.meter_serial AS meter_no,
--               event_log.meter_model,
--               event_log.event_time,
--               event_log.event_type_id,
--               event_log.event_code,
--               NULL::text AS recharge_token,
--                NULL::numeric AS recharge_amount_kwh,
--                NULL::text AS manage_token,
--                NULL::text AS manage_token_type
--        FROM event_log
--        UNION ALL
--        SELECT 'RECHARGE'::text AS src_table,
--                household_recharge_token_event.id,
--               household_recharge_token_event.meter_serial AS meter_no,
--               household_recharge_token_event.meter_model,
--               household_recharge_token_event.event_time,
--               household_recharge_token_event.event_type_id,
--               household_recharge_token_event.event_code,
--               household_recharge_token_event.recharge_token,
--               household_recharge_token_event.recharge_amount_kwh,
--               NULL::text AS text,
--                NULL::text AS text
--        FROM household_recharge_token_event
--        UNION ALL
--        SELECT 'MANAGEMENT'::text AS src_table,
--                household_management_token_event.id,
--               household_management_token_event.meter_serial AS meter_no,
--               household_management_token_event.meter_model,
--               household_management_token_event.event_time,
--               household_management_token_event.event_type_id,
--               household_management_token_event.event_code,
--               NULL::text AS text,
--                NULL::numeric AS "numeric",
--                household_management_token_event.manage_token,
--               household_management_token_event.manage_token_type
--        FROM household_management_token_event) u
--          LEFT JOIN event_type et ON et.id = u.event_type_id
--          LEFT JOIN event_code_lookup ecl ON ecl.event_type_id = u.event_type_id AND ecl.code = u.event_code;
--

--10. public.vw_12month_monthly_avg_consumption
CREATE OR REPLACE VIEW public.vw_12month_monthly_avg_consumption
 AS
SELECT meter_serial,
       meter_model,
       date_trunc('month'::text, entry_timestamp) AS month,
    round(avg(energy_consumption) OVER (PARTITION BY meter_serial ORDER BY (date_trunc('month'::text, entry_timestamp)) ROWS BETWEEN 11 PRECEDING AND CURRENT ROW), 2) AS avg_12month_consumption
FROM vw_monthly_energy_consumption;

--11. public.vw_30day_daily_avg_consumption
CREATE OR REPLACE VIEW public.vw_30day_daily_avg_consumption
 AS
SELECT meter_serial,
       meter_model,
       entry_timestamp,
       round(avg(energy_consumption) OVER (PARTITION BY meter_serial ORDER BY entry_timestamp ROWS BETWEEN 29 PRECEDING AND CURRENT ROW), 2) AS avg_30day_consumption
FROM vw_daily_energy_consumption;


--12. public.vw_daily_energy_consumption
CREATE OR REPLACE VIEW public.vw_daily_energy_consumption
 AS
 WITH readings AS (
         SELECT daily_billing_energy_hh.meter_serial,
            daily_billing_energy_hh.meter_model,
            daily_billing_energy_hh.entry_timestamp,
            daily_billing_energy_hh.active_energy_import AS current_reading,
            lag(daily_billing_energy_hh.active_energy_import) OVER (PARTITION BY daily_billing_energy_hh.meter_serial ORDER BY daily_billing_energy_hh.entry_timestamp) AS previous_reading
           FROM daily_billing_energy_hh
        )
SELECT meter_serial,
       meter_model,
       entry_timestamp,
       current_reading,
       previous_reading,
       round(
               CASE
                   WHEN previous_reading IS NULL THEN 0::double precision
            WHEN current_reading >= previous_reading THEN current_reading - previous_reading
            WHEN previous_reading >= 900000::double precision AND current_reading <= 1000::double precision THEN 999999::double precision - previous_reading + current_reading
            ELSE NULL::double precision
        END::numeric, 2) AS energy_consumption,
       CASE
           WHEN previous_reading IS NULL THEN 'INITIAL'::text
           WHEN current_reading >= previous_reading THEN 'NORMAL'::text
           WHEN previous_reading >= 900000::double precision AND current_reading <= 1000::double precision THEN 'ROLLOVER'::text
            ELSE 'INVALID_DROP'::text
END AS reading_status
   FROM readings;


--13. public.vw_monthly_energy_consumption
CREATE OR REPLACE VIEW public.vw_monthly_energy_consumption
 AS
 WITH readings AS (
         SELECT monthly_billing_energy_hh.meter_serial,
            monthly_billing_energy_hh.meter_model,
            monthly_billing_energy_hh.entry_timestamp,
            monthly_billing_energy_hh.active_energy_import AS current_reading,
            lag(monthly_billing_energy_hh.active_energy_import) OVER (PARTITION BY monthly_billing_energy_hh.meter_serial ORDER BY monthly_billing_energy_hh.entry_timestamp) AS previous_reading
           FROM monthly_billing_energy_hh
        )
SELECT meter_serial,
       meter_model,
       entry_timestamp,
       current_reading,
       previous_reading,
       round(
               CASE
                   WHEN previous_reading IS NULL THEN 0::double precision
            WHEN current_reading >= previous_reading THEN current_reading - previous_reading
            WHEN previous_reading >= 900000::double precision AND current_reading <= 1000::double precision THEN 999999::double precision - previous_reading + current_reading
            ELSE NULL::double precision
        END::numeric, 2) AS energy_consumption,
       CASE
           WHEN previous_reading IS NULL THEN 'INITIAL'::text
           WHEN current_reading >= previous_reading THEN 'NORMAL'::text
           WHEN previous_reading >= 900000::double precision AND current_reading <= 1000::double precision THEN 'ROLLOVER'::text
            ELSE 'INVALID_DROP'::text
END AS reading_status
   FROM readings;

-- 14. public.vw_flatten_node_records
CREATE OR REPLACE VIEW public.vw_flatten_node_records
 AS
SELECT r.id AS root_id,
       r.name AS root_name,
       r.email AS root_email,
       rg.node_id AS region_node_id,
       rg.parent_id AS region_parent_id,
       rg.id AS region_id,
       rg.region_id AS region_region_id,
       rg.org_id AS region_org_id,
       rg.name AS region_name,
       rg.email AS region_email,
       bh.id AS business_id,
       bh.node_id AS business_node_id,
       bh.parent_id AS business_parent_id,
       bh.org_id AS business_org_id,
       bh.region_id AS business_region_id,
       bh.name AS business_name,
       bh.email AS business_email,
       sc.id AS service_id,
       sc.node_id AS service_node_id,
       sc.parent_id AS service_parent_id,
       sc.org_id AS service_org_id,
       sc.region_id AS service_region_id,
       sc.name AS service_name,
       sc.email AS service_email,
       f.id AS feeder_id,
       f.node_id AS feeder_node_id,
       f.parent_id AS feeder_parent_id,
       f.org_id AS feeder_org_id,
       f.asset_id AS feeder_asset_id,
       f.name AS feeder_name,
       f.email AS feeder_email,
       d.id AS dss_id,
       d.node_id AS dss_node_id,
       d.parent_id AS dss_parent_id,
       d.org_id AS dss_org_id,
       d.asset_id AS dss_asset_id,
       d.name AS dss_name,
       d.email AS dss_email
FROM region_bhub_service_centers r
         LEFT JOIN region_bhub_service_centers rg ON rg.parent_id = r.node_id AND rg.type::text = 'region'::text
     LEFT JOIN region_bhub_service_centers bh ON bh.parent_id = rg.node_id AND bh.type::text = 'business hub'::text
    LEFT JOIN region_bhub_service_centers sc ON sc.parent_id = bh.node_id AND sc.type::text = 'service center'::text
    LEFT JOIN substation_trans_feeder_lines f ON f.parent_id = sc.node_id AND f.type::text = 'feeder line'::text
    LEFT JOIN substation_trans_feeder_lines d ON d.parent_id = f.node_id AND d.type::text = 'dss'::text
WHERE r.type::text = 'root'::text;