/*
 * Copyright 2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appnaut.poc.data;


import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import java.util.List;
import javax.inject.Inject;

@Controller("/data")
public class DataController {

  @Inject
  IDataService dataService;

  public DataController(IDataService dataService) {
    this.dataService = dataService;
  }

  @Get("/")
  public List<Data> index() {
    return dataService.getAllData();
  }

  @Post("/")
  public void saveData(Data data) {

    int xxx = 0;
    xxx++;

  }
}