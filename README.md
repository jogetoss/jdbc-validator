# Description

**JDBC Form Validator**

JDBC Validator allows user to perform validation by writing a SQL query. When writing the SQL query, you can retrieve form fields value by using the symbol **?** and **{fieldId}**. The **?** symbol is used to retrieve the current form field value and curly braces **{fieldId}** to retrieve other form fields' values.

## Use Case Examples
```
SELECT id FROM table WHERE id != ? OR username = ? 

SELECT id FROM table WHERE id != {id} OR username = {username}

SELECT id FROM table WHERE id != {id} OR c_year = {year} AND email = {email}
```

It does not matter what is being returned in the query as long as if there is a row returned, the validation would fail (duplicate row exists). Your query might still return 1 row (the very record that you are trying to edit/save). To ensure that we are catering to this use case, we will need to skip our own record by using `id != {id}` in the WHERE clause. For example:-

```
SELECT * FROM app_fd_item WHERE c_name = {name} AND id != {id}
```

# Getting Help

JogetOSS is a community-led team for open source software related to the [Joget](https://www.joget.org) no-code/low-code application platform.
Projects under JogetOSS are community-driven and community-supported.
To obtain support, ask questions, get answers and help others, please participate in the [Community Q&A](https://answers.joget.org/).

# Contributing

This project welcomes contributions and suggestions, please open an issue or create a pull request.

Please note that all interactions fall under our [Code of Conduct](https://github.com/jogetoss/repo-template/blob/main/CODE_OF_CONDUCT.md).

# Licensing

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

NOTE: This software may depend on other packages that may be licensed under different open source licenses.
