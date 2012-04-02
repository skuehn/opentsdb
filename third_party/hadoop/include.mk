# Copyright (C) 2011-2012  The OpenTSDB Authors.
#
# This library is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published
# by the Free Software Foundation, either version 2.1 of the License, or
# (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this library.  If not, see <http://www.gnu.org/licenses/>.

HADOOP_VERSION := 1.0.1
HADOOP := third_party/hadoop/hadoop-core-$(HADOOP_VERSION).jar
HADOOP_BASE_URL := https://repository.apache.org/content/groups/public/org/apache/hadoop/hadoop-core/$(HADOOP_VERSION)

$(HADOOP): $(HADOOP).md5
	set dummy "$(HADOOP_BASE_URL)" "$(HADOOP)"; shift; $(FETCH_DEPENDENCY)

THIRD_PARTY += $(HADOOP)
