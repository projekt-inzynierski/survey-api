CREATE TABLE respondent_data (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    identity_user_id UNIQUEIDENTIFIER,
    gender INT,
    age_category_id INT,
    occupation_category_id INT,
    education_category_id INT,
    health_condition_id INT,
    medication_use_id INT,
    life_satisfaction_id INT,
    stress_level_id INT,
    quality_of_sleep_id INT,
    greenery_area_category_id INT,
    FOREIGN KEY (identity_user_id) REFERENCES identity_user(id),
    FOREIGN KEY (age_category_id) REFERENCES age_category(id),
    FOREIGN KEY (occupation_category_id) REFERENCES occupation_category(id),
    FOREIGN KEY (education_category_id) REFERENCES education_category(id),
    FOREIGN KEY (health_condition_id) REFERENCES health_condition(id),
    FOREIGN KEY (medication_use_id) REFERENCES medication_use(id),
    FOREIGN KEY (life_satisfaction_id) REFERENCES life_satisfaction(id),
    FOREIGN KEY (stress_level_id) REFERENCES stress_level(id),
    FOREIGN KEY (quality_of_sleep_id) REFERENCES quality_of_sleep(id),
    FOREIGN KEY (greenery_area_category_id) REFERENCES greenery_area_category(id),
);
