CREATE TABLE public.meter_readings (
  record_date date NOT NULL,
  gas decimal(21,2) NOT NULL,
  electricity_normal decimal(21,2) NOT NULL,
  electricity_low decimal(21,2) NOT NULL,
  PRIMARY KEY (record_date)
);